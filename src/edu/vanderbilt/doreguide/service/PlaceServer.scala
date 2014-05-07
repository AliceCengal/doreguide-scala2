package edu.vanderbilt.doreguide.service

import java.io.{FileOutputStream, OutputStreamWriter, Reader, FileInputStream, File, InputStreamReader}
import scala.io.{Codec, Source}

import android.content.Context

import com.google.gson.JsonParser

import edu.vanderbilt.doreguide.model.Place

/**
 * Fetches and caches place data
 *
 * Created by athran on 4/15/14.
 */
object PlaceServer {

  /**
   * Fetches the Place with this Id.
   *
   * Reply: PlaceResult
   */
  case class  GetPlaceWithId(id: Int)

  /**
   * Fetches the Places with these Ids. Empty list if no ids
   * match in the database.
   *
   * Reply: PlaceResult
   */
  case class  GetPlacesIdRange(ids: List[Int])

  /**
   * Get a list of all the places we have in the database
   *
   * Reply: PlaceResult
   */
  case object GetAllPlaces

  /**
   * Find the Place closest to the given coordinate
   *
   * Reply: ClosestPlace
   */
  case class FindClosestPlace(lat: Double, lng: Double)

  /**
   * Find n number of closest Places
   *
   * Reply: List[ClosestPlace]
   */
  case class FindNClosest(lat: Double, lng: Double, n: Int)

  case class PlaceResult(plcs: List[Place])

  /**
   * Container for the closest request
   */
  case class ClosestPlace(plc: Place)

  val rawDataUrl = "https://raw.github.com/AliceCengal/vanderbilt-data/master/places.json"

  val cachePath = "/places.json"

}

private[service] class PlaceServer extends HandlerActor.Server {

  import PlaceServer._
  import scala.collection.JavaConverters._
  import Geomancer.calcDistance

  private var placeBank: Map[Int,Place] = Map.empty

  def handleRequest(req: AnyRef) {
    req match {
      case GetPlaceWithId(id)         => sendPlaceWithId(id)
      case GetPlacesIdRange(ids)      => sendPlacesWithIDs(ids)
      case GetAllPlaces               => sendAllPlaces()
      case FindClosestPlace(lat, lng) => sendClosestPlace((lat,lng))
      case FindNClosest(lat, lng, n)  => sendNClosestPlace((lat,lng), n)
      case _                          =>
    }
  }

  def init(ctx: Context): Unit = {
    val cacheFile = new File(ctx.getExternalFilesDir(null),
                             cachePath)

    if (cacheFile.exists() && cacheFile.isFile && cacheFile.canRead) {
      val plcList = readFromStream(new InputStreamReader(new FileInputStream(cacheFile)))

      if (!plcList.isEmpty) {
        placeBank =
            plcList.
                map(p => p.uniqueId -> p).
                toMap
      } else {
        loadFromServer(ctx)
      }

    } else {
      loadFromServer(ctx)
    }

  }

  private def readFromStream(reader: Reader): Seq[Place] = {
    try {
      new JsonParser().
          parse(reader).
          getAsJsonArray.
          iterator().asScala.
          map(_.getAsJsonObject).
          map(obj => Place.fromJsonObject(obj)).
          toList
    } catch {
      case e: Exception => List.empty[Place]
    }
  }

  private def loadFromServer(ctx: Context) {
    val serverJson = Source.fromURL(rawDataUrl, Codec.UTF8.name)
    val cache = new OutputStreamWriter(
                    new FileOutputStream(
                        new File(ctx.getExternalFilesDir(null),
                                 cachePath)))
    for (l <- serverJson.getLines()) { cache.write(l) }
    serverJson.close()
    cache.flush()
    cache.close()

    val plcs = readFromStream(Source.fromFile(new File(ctx.getExternalFilesDir(null),
                                                       cachePath),
                                              Codec.UTF8.name).
                                  reader())
    placeBank = plcs.
                    map(p => p.uniqueId -> p).
                    toMap
  }

  private def sendPlaceWithId(id: Int) {
    val maybePlc = placeBank.get(id)
    requester ! PlaceResult(if (maybePlc.isDefined) List(maybePlc.get) else List())
  }

  private def sendPlacesWithIDs(ids: List[Int]) {
    requester ! PlaceResult(
                             ids.
                             map(id => placeBank.get(id)).
                             filter(maybePlace => maybePlace.isDefined).
                             map(maybePlace => maybePlace.get)
                           )
  }

  private def sendAllPlaces() {
    requester ! PlaceResult(placeBank.values.toList)
  }

  private def sendClosestPlace(coordinate: (Double,Double)) {

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

    requester ! PlaceResult(List(closest))
  }

  private def sendNClosestPlace(coordinate: (Double,Double),
                                n: Int) {
    def distanceToReference(plc: Place): Double = {
      calcDistance(
                    plc.latitude,
                    plc.longitude,
                    coordinate._1,
                    coordinate._2)
    }

    val placeList = placeBank.values.toList
    val closestN = placeList.
        sortWith(distanceToReference(_) <
                 distanceToReference(_)).
        take(n)

    requester ! PlaceResult(closestN)
  }

}
