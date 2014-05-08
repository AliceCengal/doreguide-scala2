package edu.vanderbilt.doreguide

import scala.collection.mutable

import android.app.Fragment
import android.os.{Message, Handler}
import android.widget.TextView
import android.view.View
import android.view.View.OnClickListener

import com.google.android.gms.maps.model.{BitmapDescriptorFactory, Marker, LatLng, MarkerOptions}
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.{CameraUpdateFactory, MapFragment}

import edu.vanderbilt.doreguide.model.Place
import edu.vanderbilt.doreguide.view.{SimpleInjections, FragmentViewUtil}
import edu.vanderbilt.doreguide.PlacesMapFragment._
import service._

/**
 * The map with its underbar
 *
 * Created by athran on 4/20/14.
 */
class PlacesMapFragment extends MapFragment
                                with SimpleInjections.FragmentInjection
                                with Handler.Callback
{
  self: MapBehaviour =>

  val markers = mutable.Set.empty[(Place, Marker)]

  def handleMessage(msg: Message): Boolean = {
    handleSpecial(msg.obj)
    true
  }

  override def onResume() {
    super.onResume()
    init()
  }

}

object PlacesMapFragment {

  lazy val normalIcon = BitmapDescriptorFactory.defaultMarker()
  lazy val highlighted = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)

  case class MarkerClicked(plc: Place)

  def showHearted = {
    new PlacesMapFragment with ShowHearted
  }

  trait MapBehaviour {
    def init()
    def handleSpecial(msg: AnyRef) {}
  }

  trait ShowHearted extends MapBehaviour with GoogleMap.OnMarkerClickListener {
    self: PlacesMapFragment =>

    override def init() {
      val googleMap = getMap

      googleMap.moveCamera(CameraUpdateFactory.
                           newLatLngZoom(new LatLng(Geomancer.DEFAULT_LATITUDE,
                                                    Geomancer.DEFAULT_LONGITUDE),
                                         Geomancer.DEFAULT_ZOOM))
      for (plc <- dore.getAllHearted) {
        val option = new MarkerOptions().
                    draggable(false).
                    position(new LatLng(plc.latitude,
                                         plc.longitude)).
                    title(plc.name)
        val marker = googleMap.addMarker(option)
        marker.setSnippet(plc.name)
        markers.add((plc, marker)) // terrible API
      }

      googleMap.setOnMarkerClickListener(this)
    }

    override def onMarkerClick(marker: Marker): Boolean = {
      for (placeClicked <- markers.find(pair => pair._1.name.equals(marker.getSnippet))) {
        dore.eventbus ! MarkerClicked(placeClicked._1)
      }

      getMap.animateCamera(CameraUpdateFactory.
                              newLatLng(marker.getPosition))

      markers.foreach(pair => pair._2.setIcon(normalIcon))
      marker.setIcon(highlighted)
      true
    }

  }

  trait ShowAll extends MapBehaviour with GoogleMap.OnMapClickListener {
    self: PlacesMapFragment =>

    override def init() {

    }
  }

}

class MapUnderbarFrag extends Fragment
                              with SimpleInjections.FragmentInjection
                              with FragmentViewUtil
                              with Handler.Callback {

  import MapUnderbarFrag._

  def layoutId: Int = R.layout.underbar_text

  lazy val controller = HandlerActor.sync(this)
  var place: Place = null

  def box = component[TextView](R.id.tv_title)

  override def onStart() {
    super.onStart()
    dore.eventbus ! EventBus.Subscribe(controller)
    box.setOnClickListener(new OnClickListener {
      def onClick(p1: View): Unit = {
        dore.eventbus ! MapUnderbarFrag.MapUnderbarClicked(place)
      }
    })
  }

  override def onStop() {
    super.onStop()
    dore.eventbus ! EventBus.Unsubscribe(controller)
  }

  def handleMessage(msg: Message): Boolean = {
    msg.obj match {
      case PlacesMapFragment.MarkerClicked(plc) =>
        display(plc)
      case Display(plc) =>
        display(plc)
      case _ =>
    }
    true
  }

  def display(plc: Place) {
    place = plc
    box.setText(plc.name)
  }

}

object MapUnderbarFrag {
  case class MapUnderbarClicked(plc: Place)
  case class Display(plc: Place)
}