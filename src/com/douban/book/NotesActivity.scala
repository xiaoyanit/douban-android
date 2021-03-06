package com.douban.book

import android.app.Activity
import android.content.{Context, Intent}
import android.os.Bundle
import android.view._
import android.view.inputmethod.InputMethodManager
import android.widget._
import com.douban.base._
import com.douban.models.{Annotation, AnnotationSearch, AnnotationSearchResult, _}
import org.scaloid.common._

/**
 * Copyright by <a href="http://www.josephjctang.com"><em><i>Joseph J.C. Tang</i></em></a> <br/>
 * Email: <a href="mailto:jinntrance@gmail.com">jinntrance@gmail.com</a>
 * @author joseph
 * @since 10/2/13 9:32 PM
 * @version 1.0
 */
class NotesActivity extends DoubanActivity {
  var listAdapter: NoteItemAdapter = null
  private val REQUEST_CODE = 0

  def viewNote(pos: Int) {
    storeData(listAdapter.getItems)
    startActivityForResult(SIntent[PublicNoteViewActivity].putExtra(Constant.ARG_POSITION, pos), REQUEST_CODE)
  }

  override def onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
      val p = data.getIntExtra(Constant.ARG_POSITION, -1)
      if (-1 != p)
        listFragment.setSelection(p)
    }
  }

  lazy val bookId = getIntent.getLongExtra(Constant.BOOK_ID, 0)
  private lazy val listFragment: NotesListFragment = new NotesListFragment()

  protected override def onCreate(b: Bundle) {
    super.onCreate(b)
    if (0 == bookId) finish()
    setContentView(R.layout.notes)
    b match {
      case savedInstance: Bundle =>
      case _ => fragmentManager.beginTransaction().replace(R.id.notes_container, listFragment).commit()
    }

  }

  override def onCreateOptionsMenu(menu: Menu) = {
    getMenuInflater.inflate(R.menu.add_note, menu)
    super.onCreateOptionsMenu(menu)
  }

  def search(v: View) = listFragment.searchByViewId(v.getId)

  override def back(v: View) {
    listFragment.bookPage = ""
    find[EditText](R.id.bookPage).setText("")
    super.back(v)
  }

  def hidePopup(v: View) = {
    page.clearFocus()
    getSystemService(Context.INPUT_METHOD_SERVICE) match {
      case m: InputMethodManager => m.hideSoftInputFromWindow(page.getWindowToken, 0)
      case _ =>
    }
    hideWhen(R.id.page_num_popup, condition = true)
  }

  lazy val page = find[EditText](R.id.bookPage)

  def showPopup(v: View) = {
    toggleDisplayWhen(R.id.page_num_popup, condition = true)
    page.requestFocus()
    getSystemService(Context.INPUT_METHOD_SERVICE) match {
      case m: InputMethodManager => m.showSoftInput(page, InputMethodManager.SHOW_IMPLICIT)
      case _ =>
    }
  }

  def addNote(m: MenuItem) = listFragment match {
    case l: NotesListFragment => l.addNote()
    case _ =>
  }

  def addNote(v: View) = listFragment match {
    case l: NotesListFragment => l.addNote()
    case _ =>
  }
}

class NotesListFragment extends DoubanListFragment[NotesActivity] {

  import com.douban.book.R.id._

  lazy val mapping = NotesActivity.mapping ++ Map(user_avatar ->("author_user.avatar", ("author_user.name", getString(R.string.load_img_fail))))
  var currentPage = 1
  var total = Int.MaxValue
  var rank = "rank"
  var bookPage = ""
  lazy val adapter: NoteItemAdapter = new NoteItemAdapter(mapping, search())


  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    inflater.inflate(R.layout.notes_list, container, false)
  }

  override def onActivityCreated(b: Bundle) {
    super.onActivityCreated(b)
    setListAdapter(adapter)
    activity.listAdapter = adapter
    getListView.setDivider(getResources.getDrawable(R.drawable.divider))
    getListView.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS)
    activity.restoreDefaultActionBar()
    activity.getIntent.getExtras.getString(Constant.BOOK_PAGE) match {
      case s: String =>
        bookPage = s
        getView.findViewById(R.id.notes_list_header).setVisibility(View.GONE)
      case _ =>
        getView.find[ImageView](R.id.forward).onClick { _: (View) =>
          val bookPage = getView.find[EditText](R.id.bookPage).getText.toString
          startActivity(SIntent[NotesActivity].putExtra(Constant.BOOK_ID, activity.bookId).
            putExtra(Constant.BOOK_PAGE, bookPage))
          activity.hidePopup(null)
        }
    }
    search()
  }

  def search(bookId: Long = activity.bookId, order: String = rank, page: Int = currentPage, bookPage: String = bookPage) = {
    listLoader(
      toLoad = 1 == page || adapter.count < total,
      result = Book.annotationsOf(bookId, new AnnotationSearch(order, (page - 1) * countPerPage, countPerPage, bookPage)),
      success = (a: AnnotationSearchResult) => {
        val size: Int = a.annotations.size
        total = a.total
        val index = a.start + size
        currentPage += 1
        runOnUiThread {
          if (1 == page) {
            adapter.replaceResult(a.total, size, a.annotations)
            adapter.notifyDataSetInvalidated()
          } else {
            adapter.addResult(a.total, size, a.annotations)
            adapter.notifyDataSetChanged()
          }
          activity.setTitle(getString(R.string.annotation) + s"($index/$total)")
          val l = activity.find[View](R.id.note_to_add)
          l.setVisibility(if (0 == total) View.VISIBLE else View.GONE)
          if (0 == total) {
            fragmentManager.beginTransaction().hide(this)
            if (bookPage.isEmpty) l.find[TextView](R.id.note2add_text).setText(getString(R.string.add_note_no_page, bookPage))
          }
          else fragmentManager.beginTransaction().show(this)
        }
        if (index < total) toast(getString(R.string.more_notes_loaded).format(index))
        else toast(R.string.more_loaded_finished)
      }
    )
  }

  def addNote() {
    activity.startActivity(SIntent[AddNoteActivity].putExtra(Constant.BOOK_ID, activity.bookId).
      putExtra(Constant.BOOK_PAGE, if (bookPage.trim.nonEmpty) bookPage.trim else null))
  }

  def searchByViewId(viewId: Int) {
    val order = Map(R.id.rank -> "rank", R.id.collect -> "collect", R.id.page -> "page")
    viewId match {
      case id: Int if rank != order.getOrElse(id, "rank") =>
        order.keys.foreach(i => activity.toggleBackGround(i != id, i, (R.color.black, R.color.black_light)))
        currentPage = 1
        rank = order(id)
        search(page = 1)
      case _ =>
    }
  }

  override def onListItemClick(l: ListView, v: View, position: Int, id: Long) {
    l.setItemChecked(position, true)
    activity.viewNote(position)
  }
}

object NotesActivity {

  import com.douban.book.R.id._

  val mapping = Map(page_num ->("page_no", "P%s"), chapter_name -> "chapter", note_time -> "time", username -> "author_user.name", note_content -> "content")
}

class NoteItemAdapter(mapping: Map[Int, Any], load: => Unit, layout: Int = R.layout.notes_item)(implicit ctx: DoubanActivity) extends ItemAdapter[Annotation](layout, mapping, load = load) with Serializable {
  override def getView(position: Int, view: View, parent: ViewGroup): View = {
    val convertView = super.getView(position, view, parent)
    getItem(position).page_no match {
      case i: Int => ctx.hideWhen(R.id.chapter_name, condition = true, convertView)
    }
    convertView
  }
}
