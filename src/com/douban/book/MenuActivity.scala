package com.douban.book

import com.douban.base.{DoubanActivity, DoubanFragment}
import android.view.{View, ViewGroup, LayoutInflater}
import android.os.Bundle
import scala.concurrent._
import com.douban.models.{Book, User}
import scala.util.Success
import org.scaloid.common._
import android.widget.LinearLayout
import ExecutionContext.Implicits.global


/**
 * Copyright by <a href="http://crazyadam.net"><em><i>Joseph J.C. Tang</i></em></a> <br/>
 * Email: <a href="mailto:jinntrance@gmail.com">jinntrance@gmail.com</a>
 * @author joseph
 * @since 10/7/13 3:24 PM
 * @version 1.0
 */
class MenuActivity {
}

class MenuFragment extends DoubanFragment[DoubanActivity]{
  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = inflater.inflate(R.layout.menu,container,false)

  override def onActivityCreated(b: Bundle) {
    super.onActivityCreated(b)
  }



}