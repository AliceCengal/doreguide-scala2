package edu.vanderbilt.doreguide.service

import android.os.{Message, Handler}
import java.text.DecimalFormat
import android.location.{Criteria, LocationManager, Location}
import edu.vanderbilt.doreguide.service.Dore.Initialize
import android.content.Context

/**
 * Does all location related stuff
 *
 * Created by athran on 4/15/14.
 */
object Geomancer {
  val DEFAULT_LONGITUDE = -86.803889
  val DEFAULT_LATITUDE  = 36.147381
  val DEFAULT_LOC = {
    val l = new Location("default")
    l.setLatitude(DEFAULT_LATITUDE)
    l.setLongitude(DEFAULT_LONGITUDE)
    l
  }

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

  def defaultCriteria = {
    val crit = new Criteria()
    crit setAccuracy         Criteria.ACCURACY_FINE
    crit setAltitudeRequired false
    crit setBearingRequired  false
    crit setSpeedRequired    false
    crit setCostAllowed      true
    crit
  }

  /**
   * Request current location
   *
   * Reply: Option[Location]
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

private[service] class Geomancer extends Handler.Callback {

  import Geomancer._

  private var locationManager: LocationManager = null
  private var provider: String = null
  private var serviceStatus: LocationServiceStatus = Disabled

  def handleMessage(msg: Message): Boolean = {
    msg.obj match {
      case Initialize(ctx)                        => init(ctx)
      case (requester: HandlerActor, GetLocation) => replyLocation(requester)
      case (requester: HandlerActor, GetStatus)   => replyStatus(requester)
    }
    true
  }

  private def init(ctx: Context): Unit = {
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
    requester ! Option(loc)
  }

  private def replyStatus(requester: HandlerActor): Unit = {
    requester ! serviceStatus
  }

}
