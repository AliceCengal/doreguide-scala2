package edu.vanderbilt.doreguide

import android.app.Fragment
import android.os.{Message, Handler}
import android.widget.TextView
import android.view.View
import edu.vanderbilt.doreguide.service.{HandlerActor, Geomancer}
import edu.vanderbilt.doreguide.model.Place
import edu.vanderbilt.doreguide.service.EventBus.{Unsubscribe, Subscribe}
import android.view.View.OnClickListener
import edu.vanderbilt.doreguide.MapUnderbarFrag.Display

/**
 * Created by athran on 4/20/14.
 */
class PlacesMapFragment extends Fragment
                                with SimpleInjections.FragmentInjection
                                with FragmentViewUtil
                                with Handler.Callback {

  import PlacesMapFragment._
  import Geomancer._

  def layoutId: Int = R.layout.simple_text

  def handleMessage(p1: Message): Boolean = {
    true
  }

  override def onStart() {
    super.onStart()

    component[TextView](R.id.tv_title).setOnClickListener(new View.OnClickListener() {
      def onClick(p1: View): Unit = {
        dore.eventbus ! MarkerClicked(DEFAULT_LATITUDE,
                                      DEFAULT_LONGITUDE)
      }
    })
  }

}

object PlacesMapFragment {
  case class MarkerClicked(lat: Double, lng: Double)
}

class MapUnderbarFrag extends Fragment
                              with SimpleInjections.FragmentInjection
                              with FragmentViewUtil
                              with Handler.Callback {

  def layoutId: Int = R.layout.simple_text

  lazy val controller = new HandlerActor(getActivity.getMainLooper, this)
  var place: Place = null

  def box = component[TextView](R.id.tv_title)

  override def onStart() {
    super.onStart()
    dore.eventbus ! Subscribe(controller)
    box.setOnClickListener(new OnClickListener {
      def onClick(p1: View): Unit = {
        dore.eventbus ! MapUnderbarFrag.MapUnderbarClicked(place)
      }
    })
  }

  override def onStop() {
    super.onStop()
    dore.eventbus ! Unsubscribe(controller)
  }

  def handleMessage(msg: Message): Boolean = {
    msg.obj match {
      case Display(plc) =>
        place = plc
        box.setText(plc.name)
      case _ =>
    }
    true
  }
}

object MapUnderbarFrag {
  case class MapUnderbarClicked(plc: Place)
  case class Display(plc: Place)
}