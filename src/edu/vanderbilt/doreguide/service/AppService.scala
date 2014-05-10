package edu.vanderbilt.doreguide.service

import scala.collection.mutable

import android.content.Context
import android.os.{Message, Handler, HandlerThread}

import edu.vanderbilt.doreguide.model.Place
import edu.vanderbilt.doreguide.service.AppService._
import android.app.{Activity, Fragment}

/**
 * The starting point of the app process. Initiates the services.
 *
 * Created by athran on 4/15/14.
 */
class AppService extends android.app.Application
                         with HeartPersistence
                         with ActorConversion
{
  private val IMAGE_SERVER_INDEX = 0
  private val PLACE_SERVER_INDEX = 1
  private val GEOMANCER_INDEX    = 2
  private val FEEDBACK_INDEX     = 3

  private var handles        = List.empty[Handler]
  private val eventbusHandle = new Handler(new EventHub)
  private val heartedSet     = mutable.Set.empty[Place]

  override def onCreate() {
    super.onCreate()
    initializeGlobalState()
  }

  def imageServer: Handler = handles(IMAGE_SERVER_INDEX)

  def placeServer: Handler = handles(PLACE_SERVER_INDEX)

  def geomancer: Handler   = handles(GEOMANCER_INDEX)

  def feedback: Handler    = handles(FEEDBACK_INDEX)

  def eventbus: Handler    = eventbusHandle

  def eventHub: Handler    = eventbusHandle

  def heart(plc: Place): Unit = {
    heartedSet.add(plc)
    saveHearted(heartedSet.
                map(p => p.uniqueId).
                toList)
  }

  def unheart(plc: Place): Unit = {
    heartedSet.remove(plc)
    saveHearted(heartedSet.
                map(p => p.uniqueId).
                toList)
  }

  def isHearted(plc: Place): Boolean = {
    heartedSet.contains(plc)
  }

  def getAllHearted: Seq[Place] = heartedSet.toList

  private def initializeGlobalState() {
    val thread = new HandlerThread("workerthread")
    thread.start()

    handles =
        List(
          new ImageServer,
          new PlaceServer,
          new Geomancer,
          new FeedbackServer).
            map(
              callback =>
                new Handler(thread.getLooper, callback))

    handles foreach { _ ! Initialize(this) }

    val heartedIds = loadHearted()
    if (!heartedIds.isEmpty) {
      handles(this.PLACE_SERVER_INDEX).
      request(
            PlaceServer.GetPlacesIdRange(heartedIds))(
            new Handler(new PlaceReceiver))
    }
  }

  class PlaceReceiver extends Handler.Callback {
    def handleMessage(msg: Message): Boolean = {
      msg.obj match {
        case PlaceServer.PlaceResult(plcs) =>
          heartedSet ++= plcs
        case _ =>
      }
      true
    }
  }

}

object AppService {

  val PREFS = "doreguide"

  private[service] case class Initialize(ctx: Context)

  trait HeartPersistence {
    self: android.app.Application =>

    val HEARTED = "hearted"

    def getPrefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    def saveHearted(hearteds: List[Int]) {
      getPrefs.
          edit().
          putString(HEARTED,
                    hearteds.mkString(",")).
          commit()
    }

    def loadHearted(): List[Int] = {
      getPrefs.
          getString(HEARTED,
                    "").
          split(",").
          filter(s => !s.isEmpty).
          map(numString => Integer.parseInt(numString)).
          toList
    }

  }

  /**
   * Allow easy access to the Application object in Activity
   */
  trait ActivityInjection {
    self: Activity =>

    def app = self.getApplication.asInstanceOf[AppService]

  }

  /**
   * Allow easy access to the Application object in Fragment
   */
  trait FragmentInjection {
    self: Fragment =>

    def app = self.getActivity.getApplication.asInstanceOf[AppService]

  }

}
