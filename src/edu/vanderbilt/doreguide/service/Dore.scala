package edu.vanderbilt.doreguide.service

import scala.collection.mutable

import android.content.Context
import android.os.{Message, Handler, HandlerThread}

import edu.vanderbilt.doreguide.model.Place
import edu.vanderbilt.doreguide.service.Dore.HeartPersistence

/**
 * The starting point of the app process. Initiates the services.
 *
 * Created by athran on 4/15/14.
 */
class Dore extends android.app.Application with HeartPersistence {

  import Dore.Initialize

  private val IMAGE_SERVER_INDEX = 0
  private val PLACE_SERVER_INDEX = 1
  private val GEOMANCER_INDEX    = 2
  private val FEEDBACK_INDEX     = 3

  private var handles        = List.empty[HandlerActor]
  private val eventbusHandle = HandlerActor.sync(new EventBus)
  private val heartedSet     = mutable.Set.empty[Place]

  override def onCreate() {
    super.onCreate()
    initializeGlobalState()
  }

  def imageServer: HandlerActor = handles(IMAGE_SERVER_INDEX)

  def placeServer: HandlerActor = handles(PLACE_SERVER_INDEX)

  def geomancer: HandlerActor   = handles(GEOMANCER_INDEX)

  def feedback: HandlerActor    = handles(FEEDBACK_INDEX)

  def eventbus: HandlerActor    = eventbusHandle

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
                HandlerActor.async(
                  thread.getLooper,
                  callback))

    handles foreach { _ ! Initialize(this) }

    val heartedIds = loadHearted()
    if (!heartedIds.isEmpty) {
      handles(this.PLACE_SERVER_INDEX).
      request(
            PlaceServer.GetPlacesIdRange(heartedIds))(
            HandlerActor.sync(new PlaceReceiver))
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

private[service] object Dore {

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

}
