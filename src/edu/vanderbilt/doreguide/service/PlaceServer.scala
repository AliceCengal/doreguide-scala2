package edu.vanderbilt.doreguide.service

import android.os.{Message, Handler}
import edu.vanderbilt.doreguide.model.Place
import com.google.gson.JsonParser
import java.net.URL
import java.io.InputStreamReader

/**
 * Fetches and caches place data
 *
 * Created by athran on 4/15/14.
 */
private[service] class PlaceServer extends Handler.Callback {

  import Dore._
  import PlaceServer._
  import scala.collection.JavaConverters._

  private var placeBank: Map[Int,Place] = Map.empty

  def handleMessage(msg: Message): Boolean = {
    msg.obj match {
      case Initialize(ctx)                               => init()
      case (r: HandlerActor, GetPlaceWithId(id))         => sendPlaceWithId(r, id)
      case (r: HandlerActor, GetPlacesIdRange(ids))      => sendPlacesWithIDs(r, ids)
      case (r: HandlerActor, GetAllPlaces)               => sendAllPlaces(r)
      case (r: HandlerActor, FindClosestPlace(lat, lng)) => sendClosestPlace(r, (lat,lng))
    }
    true
  }

  private def init(): Unit = {
    this.placeBank =
      new JsonParser()
          .parse(
            new InputStreamReader(
              new URL(rawDataUrl)
                  .openConnection()
                  .getInputStream))
          .getAsJsonArray
          .iterator().asScala
          .map(_.getAsJsonObject)
          .map(obj => Place.fromJsonObject(obj))
          .map(plc => (plc.uniqueId, plc))
          .toMap
  }

  private def sendPlaceWithId(requester: HandlerActor, id: Int) {
    requester ! placeBank(id)
  }

  private def sendPlacesWithIDs(requester: HandlerActor, ids: List[Int]) {
    requester ! ids.map(id => placeBank(id))
  }

  private def sendAllPlaces(requester: HandlerActor) {
    requester ! placeBank.values.toList
  }

  private def sendClosestPlace(requester: HandlerActor, coordinate: (Double,Double)) {

    import Geomancer.calcDistance

    def distanceToReference(plc: Place): Double = {
      calcDistance(
        plc.latitude,
        plc.longitude,
        coordinate._1,
        coordinate._2)
    }

    val placeList = placeBank.values.toList
    val closest = placeList.
        sortWith(
          (plc1, plc2) => (
              distanceToReference(plc1)
                  <
                  distanceToReference(plc2))).
        apply(0)

    requester ! closest
  }

}

object PlaceServer {
  case class  GetPlaceWithId(id: Int)
  case class  GetPlacesIdRange(ids: List[Int])
  case object GetAllPlaces

  case class FindClosestPlace(lat: Double, lng: Double)
  case class ClosestPlace(plc: Place)

  val rawDataUrl = "https://raw.github.com/AliceCengal/vanderbilt-data/master/places.json"
}