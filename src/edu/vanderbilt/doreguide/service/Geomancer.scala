package edu.vanderbilt.doreguide.service

import java.text.DecimalFormat
import android.location.{Criteria, LocationManager, Location}
import android.content.Context
import edu.vanderbilt.doreguide.view.EasyChainCall

/**
 * Does all location related stuff
 *
 * Created by athran on 4/15/14.
 */
object Geomancer extends EasyChainCall {
  val DEFAULT_LONGITUDE = -86.803889
  val DEFAULT_LATITUDE  = 36.147381
  val DEFAULT_LOC       = initDefaultLocation
  val DEFAULT_ZOOM      = 14.5f

  val FEET_PER_METER = 3.28083989501312
  val FEET_PER_MILE  = 5280

  val DEFAULT_TIMEOUT = 5000
  val DEFAULT_RADIUS  = 5

  val EARTH_RADIUS = 6378100 // meters

  /**
   * For displaying a distance value with proper units
   */
  def getDistanceString(distanceInMeter: Double): String = {
    val distanceInFeet = distanceInMeter * FEET_PER_METER
    if (distanceInFeet < 1000)
      distanceInFeet.toInt.toString + " ft"
    else
      new DecimalFormat("#.##").format(distanceInFeet / FEET_PER_MILE) + " mi"
  }

  def calcDistance(lat1: Double,
                   lng1: Double,
                   lat2: Double,
                   lng2: Double): Double = {
    import Math._

    val lat1R = toRadians(lat1)
    val lat2R = toRadians(lat2)
    val dLatR = abs(lat2R - lat1R)
    val dLngR = abs(toRadians(lng2 - lng1))
    val a = pow(sin(dLatR / 2), 2) +
            cos(lat1R) * cos(lat2R) * sin(dLngR / 2) * sin(dLngR / 2)

    2 * atan2(sqrt(a), sqrt(1 - a)) * EARTH_RADIUS
  }

  /**
   * One of the most beautiful thing in the world.
   */
  def defaultCriteria = {
    new Criteria <<< (
        _ setBearingRequired  false,
        _ setSpeedRequired    false,
        _ setCostAllowed      true,
        _ setAltitudeRequired false,
        _ setAccuracy         Criteria.ACCURACY_FINE)
  }

  def initDefaultLocation = {
    new Location("default") <<< (
        _ setLatitude DEFAULT_LATITUDE,
        _ setLongitude DEFAULT_LONGITUDE)
  }

  /**
   * Request current location
   *
   * Reply: LocationResult
   */
  case object GetLocation

  /**
   * Request current status of location service
   *
   * Reply: LocationServiceStatus
   */
  case object GetStatus

  case object UpdateLocation

  case object TimerStop

  case class LocationResult(maybeLoc: Option[Location])

  /**
   * Indicates the current status of the location service
   */
  sealed abstract class LocationServiceStatus

  /**
   * Location service is unavailable
   */
  case object Disabled extends LocationServiceStatus

  /**
   * Location service is available
   */
  case object Enabled  extends LocationServiceStatus

}

private[service] class Geomancer extends HandlerActor.Server {

  import Geomancer._

  private var locationManager: LocationManager       = null
  private var provider:        String                = null
  private var serviceStatus:   LocationServiceStatus = Disabled

  def handleRequest(req: AnyRef) {
    req match {
      case GetLocation => replyLocation(requester)
      case GetStatus   => replyStatus(requester)
      case _           =>
    }
  }

  def init(ctx: Context): Unit = {
    locationManager =
        ctx.
            getSystemService(Context.LOCATION_SERVICE).
            asInstanceOf[LocationManager]

    provider = locationManager.getBestProvider(defaultCriteria, true)

    if (provider != null) {
      serviceStatus = Enabled
    }
  }

  private def replyLocation(requester: HandlerActor): Unit = {
    val loc = locationManager.getLastKnownLocation(provider)
    requester ! LocationResult(Option(loc))
  }

  private def replyStatus(requester: HandlerActor): Unit = {
    requester ! serviceStatus
  }

}
