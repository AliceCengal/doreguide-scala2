package edu.vanderbilt.doreguide.service

import scala.collection.mutable
import scala.ref.WeakReference
import android.os.{Message, Handler}

/**
 * The global event bus that allows components to communicate
 * with each other
 *
 * Created by athran on 4/16/14.
 */
object EventBus {

  /**
   * Subscribe to the global event stream
   */
  case class Subscribe(who: HandlerActor)

  /**
   * Unsubscribe from the global event stream
   */
  case class Unsubscribe(who: HandlerActor)

  /**
   * Subscribe only to events of these types
   *
   * Not implemented for now. Probably won't be for a while,
   * unless there is a big performance issue with sending all
   * messages to all subscribers.
   */
  //case class SubscribeForEvents(who: HandlerActor, eventTypes: Set[Class])

}

private[service] class EventBus extends Handler.Callback {

  import EventBus._

  private val subscribers = mutable.Set.empty[WeakReference[HandlerActor]]

  def handleMessage(incoming: Message): Boolean = {
    incoming.obj match {
      case Subscribe(who)   => subscribers.add(new WeakReference[HandlerActor](who))
      case Unsubscribe(who) => purgeSubscribers(who)
      case a: AnyRef        => broadcastEvent(a)
    }
    true
  }

  def purgeSubscribers(who: HandlerActor): Unit = {
    subscribers.retain((weakHandler) => {
      weakHandler.get.isDefined && weakHandler() != who
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