package edu.vanderbilt.doreguide.service

import scala.collection.mutable
import scala.ref.WeakReference

import android.os.{Message, Handler}

/**
 * A global event hub that allows different components of
 * the app to communicate without explicit coupling.
 */
object EventHub {

  /**
   * Subscribe to the global event stream
   */
  case class Subscribe(who: Handler)

  /**
   * Unsubscribe from the global event stream
   */
  case class Unsubscribe(who: Handler)

}

private[service] class EventHub extends Handler.Callback with ActorConversion {
  import EventHub._

  private val subscribers = mutable.Set.empty[WeakReference[Handler]]

  def handleMessage(incoming: Message): Boolean = {
    incoming.obj match {
      case Subscribe(who)   => subscribers.add(new WeakReference[Handler](who))
      case Unsubscribe(who) => purgeSubscribers(who)
      case a: AnyRef        => broadcastEvent(a)
    }
    true
  }

  private def purgeSubscribers(who: Handler): Unit = {
    subscribers.retain((weakHandler) => {
      weakHandler.get.isDefined && (weakHandler() ne who)
    })
  }

  private def broadcastEvent(event: AnyRef): Unit = {
    for (
      weakRefToHandler <- subscribers;
      maybeHandler     <- weakRefToHandler.get
    ) {
      maybeHandler ! event
    }
  }

}
