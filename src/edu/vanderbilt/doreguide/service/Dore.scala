package edu.vanderbilt.doreguide.service

import android.content.Context
import android.os.HandlerThread
import roboguice.RoboGuice
import com.google.inject.{Provider, Binder, Module}

/**
 * The starting point of the app process. Initiates the services.
 *
 * Created by athran on 4/15/14.
 */
class Dore extends android.app.Application {

  import Dore._

  private val IMAGE_SERVER_INDEX = 0
  private val PLACE_SERVER_INDEX = 1
  private val GEOMANCER_INDEX    = 2

  private var handles: List[HandlerActor] = List.empty
  private val eventbusHandle: HandlerActor = new HandlerActor(this.getMainLooper, new EventBus)

  override def onCreate() {
    super.onCreate()
    initializeGlobalState()
  }

  def imageServer: HandlerActor = handles(IMAGE_SERVER_INDEX)

  def placeServer: HandlerActor = handles(PLACE_SERVER_INDEX)

  def geomancer: HandlerActor   = handles(GEOMANCER_INDEX)

  def eventbus: HandlerActor    = eventbusHandle

  private def initializeGlobalState() {
    val thread = new HandlerThread("workerthread")
    thread.start()

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

    RoboGuice.setBaseApplicationInjector(
      this,
      RoboGuice.DEFAULT_STAGE, new Module() {
        def configure(binder: Binder): Unit = {
          binder
              .bind(classOf[Dore])
              .toProvider(
                new Provider[Dore]() {
                  def get(): Dore = Dore.this
                })
        }
      })
  }

}

object Dore {

  private[service] case class Initialize(ctx: Context)

  /**
   * Indicates that a request to a Service Handler failed.
   */
  case class Failure(originalMessage: AnyRef,
                     error:           Exception,
                     extraInfo:       String)
}
