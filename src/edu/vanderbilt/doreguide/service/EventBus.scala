package edu.vanderbilt.doreguide.service

import android.os.{Message, Handler}
import scala.collection.mutable

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
  case class SubscribeForEvents(who: HandlerActor, eventTypes: Set[Class])

}

private[service] class EventBus extends Handler.Callback {

  import EventBus._

  private val subscribers: mutable.Set[HandlerActor] = mutable.Set.empty

  def handleMessage(incoming: Message): Boolean = {
    incoming.obj match {
      case Subscribe(who)   => subscribers.add(who)
      case Unsubscribe(who) => subscribers.remove(who)
      case a: AnyRef        => subscribers.foreach(_ ! a)
    }
    true
  }

}