package edu.vanderbilt.doreguide.service

import android.content.Context
import android.os.HandlerThread
import edu.vanderbilt.doreguide.model.Place
import scala.collection.mutable

/**
 * The starting point of the app process. Initiates the services.
 *
 * Created by athran on 4/15/14.
 */
class Dore extends android.app.Application {

  import Dore.Initialize

  private val IMAGE_SERVER_INDEX = 0
  private val PLACE_SERVER_INDEX = 1
  private val GEOMANCER_INDEX    = 2
  private val FEEDBACK_INDEX     = 3

  private var handles             = List.empty[HandlerActor]
  private lazy val eventbusHandle = new SyncHandlerActor(new EventBus)
  private val heartedSet          = mutable.Set.empty[Place]

  override def onCreate() {
    super.onCreate()
    initializeGlobalState()
  }

  def imageServer: HandlerActor = handles(IMAGE_SERVER_INDEX)

  def placeServer: HandlerActor = handles(PLACE_SERVER_INDEX)

  def geomancer: HandlerActor   = handles(GEOMANCER_INDEX)

  def feedback: HandlerActor    = handles(FEEDBACK_INDEX)

  def eventbus: SyncHandlerActor    = eventbusHandle

  def heart(plc: Place): Unit = {
    heartedSet.add(plc)
  }

  def unheart(plc: Place): Unit = {
    heartedSet.remove(plc)
  }

  def isHearted(plc: Place): Boolean = {
    heartedSet.contains(plc)
  }

  def getAllHearted: List[Place] = heartedSet.toList

  private def initializeGlobalState() {
    val thread = new HandlerThread("workerthread")
    thread.start()

    //eventbusHandle =

    handles =
        List(
          new ImageServer,
          new PlaceServer,
          new Geomancer,
          new FeedbackServer).
            map(
              callback =>
                new HandlerActor(
                  thread.getLooper,
                  callback))

    handles foreach { _ ! Initialize(this) }

  }

}

private[service] object Dore {

  private[service] case class Initialize(ctx: Context)

}
