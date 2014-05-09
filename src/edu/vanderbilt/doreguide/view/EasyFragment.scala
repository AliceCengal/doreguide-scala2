package edu.vanderbilt.doreguide.view

import android.app.Fragment
import android.view.{View, ViewGroup, LayoutInflater}
import android.os.Bundle

/**
 * This mixin provides automatic view inflation. Just define the
 * desired layoutId. Also provides the component method for easy
 * access to view components.
 *
 * Created by athran on 4/19/14.
 */
trait EasyFragment {
  self: Fragment =>

  def layoutId: Int

  override def onCreateView(inflater:  LayoutInflater,
                            container: ViewGroup,
                            saved:     Bundle): View = {
    inflater.inflate(layoutId, container, false)
  }

  def component[T](id: Int) = getView.findViewById(id).asInstanceOf[T]

}

trait EasyChainCall {

  class ChainCall[T](obj: T) {
    def <<<(calls: (T => Unit)*): T = {
      for (c <- calls) { c(obj) }
      obj
    }
  }

  implicit def anyrefToChainCall[U](a: U): ChainCall[U] = new ChainCall[U](a)

}
