package edu.vanderbilt.doreguide.service

import android.content.Context
import android.os.{Handler, HandlerThread}

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

  private var handles: List[HandlerActor]  = List.empty
  private var eventbusHandle: Handler with ActorComponent = null

  override def onCreate() {
    super.onCreate()
    initializeGlobalState()
  }

  def imageServer: HandlerActor = handles(IMAGE_SERVER_INDEX)

  def placeServer: HandlerActor = handles(PLACE_SERVER_INDEX)

  def geomancer: HandlerActor   = handles(GEOMANCER_INDEX)

  def eventbus: Handler with  ActorComponent = eventbusHandle

  private def initializeGlobalState() {
    val thread = new HandlerThread("workerthread")
    thread.start()

    eventbusHandle = new Handler(new EventBus) with ActorComponent

    handles =
        List(
          new ImageServer,
          new PlaceServer,
          new Geomancer).
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
