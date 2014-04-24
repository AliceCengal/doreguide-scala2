package edu.vanderbilt.doreguide.view

import android.app.{Fragment, Activity}
import edu.vanderbilt.doreguide.service.Dore

/**
 * Allow easy access to the Dore service
 *
 * Created by athran on 4/19/14.
 */
object SimpleInjections {

  trait ActivityInjection {
    self: Activity =>

    def dore = self.getApplication.asInstanceOf[Dore]

  }

  trait FragmentInjection {
    self: Fragment =>

    def dore = self.getActivity.getApplication.asInstanceOf[Dore]

  }

}
