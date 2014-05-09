package edu.vanderbilt.doreguide

import scala.collection.mutable

import android.app.Fragment
import android.os.{Message, Handler}
import android.widget.TextView
import android.view.View
import android.view.View.OnClickListener

import com.google.android.gms.maps.model.{
    CameraPosition, BitmapDescriptorFactory,
    Marker, LatLng, MarkerOptions}
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.{CameraUpdateFactory, MapFragment}

import edu.vanderbilt.doreguide.model.Place
import edu.vanderbilt.doreguide.view.{EasyChainCall, ChattyFrag, SimpleInjections, EasyFragment}
import edu.vanderbilt.doreguide.PlacesMapFragment._
import edu.vanderbilt.doreguide.service.{PlaceServer, Geomancer}

/**
 * The map with its underbar
 *
 * Created by athran on 4/20/14.
 */
class PlacesMapFragment extends MapFragment
                                with ChattyFrag
                                with SimpleInjections.FragmentInjection
                                with Handler.Callback
{
  self: MapBehaviour =>

  val markers = mutable.Set.empty[PlaceMarker]

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

  def normalIcon = BitmapDescriptorFactory.defaultMarker()
  def highlighted = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
  lazy val defaultCameraUpdate =
    CameraUpdateFactory.newLatLngZoom(new LatLng(Geomancer.DEFAULT_LATITUDE,
                                                  Geomancer.DEFAULT_LONGITUDE),
                                       Geomancer.DEFAULT_ZOOM)

  case class MarkerClicked(plc: Place)
  case class PlaceMarker(place: Place, marker: Marker)

  def showHearted = new PlacesMapFragment with ShowHearted

  def showAll = new PlacesMapFragment with ShowAll

  trait MapBehaviour {
    def init()
    def handleSpecial(msg: AnyRef) {}
  }

  trait ShowHearted extends MapBehaviour with GoogleMap.OnMarkerClickListener {
    self: PlacesMapFragment =>

    override def init() {
      val googleMap = getMap
      googleMap.getUiSettings.setZoomControlsEnabled(false)
      googleMap.moveCamera(defaultCameraUpdate)
      for (plc <- dore.getAllHearted) {
        val option = new MarkerOptions().
                    draggable(false).
                    position(new LatLng(plc.latitude,
                                         plc.longitude)).
                    title(plc.name)
        val marker = googleMap.addMarker(option)
        marker.setSnippet(plc.name)
        markers.add(PlaceMarker(plc, marker)) // terrible API
      }

      googleMap.setOnMarkerClickListener(this)
    }

    override def onMarkerClick(marker: Marker): Boolean = {
      for (PlaceMarker(place, _) <- markers.find(pm => pm.place.name.equals(marker.getSnippet))) {
        dore.eventbus ! MarkerClicked(place)
      }

      getMap.animateCamera(CameraUpdateFactory.
                              newLatLng(marker.getPosition))

      markers.foreach(pm => pm.marker.setIcon(normalIcon))
      marker.setIcon(highlighted)
      true
    }

  }

  trait ShowAll extends MapBehaviour
                        with GoogleMap.OnMarkerClickListener
                        with GoogleMap.OnCameraChangeListener
                        with EasyChainCall
  {
    self: PlacesMapFragment =>

    private val MARKER_COUNt = 20
    private val CLOSE_ZOOM   = 17f
    private var allMarkers: Seq[PlaceMarker] = List.empty

    override def init() {

      // I have absolutely no justification why this should be done this
      // way. It's just fun...
      getMap <<< (
          _.getUiSettings.setZoomControlsEnabled(false),
          _.setMyLocationEnabled(true),
          _.moveCamera(defaultCameraUpdate),
          _.setOnMarkerClickListener(this))

      dore.geomancer request Geomancer.GetLocation
    }

    override def handleSpecial(msg: AnyRef) {
      msg match {
        case Geomancer.LocationResult(maybeLoc) =>
          maybeLoc match {
            case Some(loc) =>
              dore.placeServer request
                  PlaceServer.FindNClosest(loc.getLatitude,
                                            loc.getLongitude,
                                            MARKER_COUNt)
              getMap.moveCamera(
                  CameraUpdateFactory.newLatLngZoom(
                      new LatLng(loc.getLatitude,
                                  loc.getLongitude),
                      CLOSE_ZOOM))
            case None =>
              dore.placeServer request
                  PlaceServer.FindNClosest(Geomancer.DEFAULT_LATITUDE,
                                            Geomancer.DEFAULT_LONGITUDE,
                                            MARKER_COUNt)
          }
          getMap.setOnCameraChangeListener(this)

        case PlaceServer.PlaceResult(plcs) =>
          drawMarkerForPlaces(plcs)

        case _ =>
      }
    }



    override def onMarkerClick(marker: Marker): Boolean = {
      handleMatchingMarker(marker)
      false
    }

    private def handleMatchingMarker(marker: Marker) {
      for (PlaceMarker(p, m) <- findMatchingPlaceFor(marker)) {
        dore.eventbus ! MarkerClicked(p)
      }
    }

    private def drawMarkerForPlaces(plcs: Seq[Place]) {
      allMarkers = for (plc <- plcs) yield {
        val option = new MarkerOptions().
                     draggable(false).
                     position(new LatLng(plc.latitude,
                                          plc.longitude)).
                     snippet(plc.name)
        PlaceMarker(plc, getMap.addMarker(option))
      }
    }

    override def onCameraChange(position: CameraPosition) {
      safeClear()
      dore.placeServer request PlaceServer.FindNClosest(position.target.latitude,
                                                         position.target.longitude,
                                                         MARKER_COUNt)
    }

    private def safeClear() {
      try {
        getMap.clear()
      } catch {
        case bogus: IllegalArgumentException => /* Do nothing */
        case e: Throwable => throw e
      }
    }

    private def findMatchingPlaceFor(marker: Marker): Option[PlaceMarker] = {
      allMarkers.find { pm =>
        (pm.place.name == marker.getSnippet) || (pm.marker eq marker)
      }
    }

  }

}

class MapUnderbarFrag extends Fragment
                              with ChattyFrag
                              with SimpleInjections.FragmentInjection
                              with EasyFragment
                              with Handler.Callback
{
  import MapUnderbarFrag._

  def layoutId: Int = R.layout.underbar_text

  private var place: Option[Place] = None

  private def box = component[TextView](R.id.tv_title)

  override def onStart() {
    super.onStart()
    box.setOnClickListener(new OnClickListener {
      def onClick(p1: View): Unit = {
        for (maybePlace <- place) {
          dore.eventbus ! MapUnderbarFrag.MapUnderbarClicked(maybePlace)
        }
      }
    })
  }

  def handleMessage(msg: Message): Boolean = {
    msg.obj match {
      case PlacesMapFragment.MarkerClicked(plc) =>
        display(plc)
      case Display(plc) =>
        display(plc)
      case Clear =>
        place = None
        box.setText(dore.getString(R.string.campus))
      case _ =>
    }
    true
  }

  def display(plc: Place) {
    place = Option(plc)
    for (p <- place) {box.setText(p.name)}
  }

}

object MapUnderbarFrag {
  case class MapUnderbarClicked(plc: Place)
  case class Display(plc: Place)
  case object Clear
}