package edu.vanderbilt.doreguide

import android.app.Fragment
import android.os.{Message, Handler}
import edu.vanderbilt.doreguide.service.AppService.FragmentInjection
import edu.vanderbilt.doreguide.service.Helpers.EasyFragment

/**
 * Created by athran on 4/23/14.
 */
class SettingsFrag extends Fragment
                           with EasyFragment
                           with FragmentInjection
                           with Handler.Callback {
  def layoutId: Int = 0

  def handleMessage(msg: Message): Boolean = {
    true
  }
}
