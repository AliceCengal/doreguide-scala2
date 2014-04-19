package edu.vanderbilt.doreguide

import android.app.Fragment

/**
 * Created by athran on 4/19/14.
 */
trait FragmentViewUtil {
  self: Fragment =>

  def component[T](id: Int) = getView.findViewById(id).asInstanceOf[T]

}
