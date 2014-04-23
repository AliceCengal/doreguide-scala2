package edu.vanderbilt.doreguide

import android.app.Fragment
import android.view.{View, ViewGroup, LayoutInflater}
import android.os.Bundle

/**
 * Created by athran on 4/19/14.
 */
trait FragmentViewUtil {
  self: Fragment =>

  def layoutId: Int

  override def onCreateView(inflater:  LayoutInflater,
                            container: ViewGroup,
                            saved:     Bundle): View = {
    inflater.inflate(layoutId, container, false)
  }

  def component[T](id: Int) = getView.findViewById(id).asInstanceOf[T]

}
