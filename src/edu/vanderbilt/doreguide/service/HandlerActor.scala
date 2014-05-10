package edu.vanderbilt.doreguide.service

import android.os.{Message, Handler}
import android.app.{Activity, Fragment}
import android.content.Context

/**
 * A basic Actor that uses Android's messaging framework.
 */
trait HandlerActor {
  def !(msg: AnyRef): Unit
  def ?(msg: AnyRef)(implicit requester: Handler): Unit
  def tell(msg: AnyRef): Unit
  def request(msg: AnyRef)(implicit requester: Handler): Unit
}

/**
 * A wrapper around Android's Handler which adds convenient
 * Erlang-inspired Actor DSL. Using implicit conversion by mixing
 * in the ActorConversion trait, a normal Handler can be treated
 * as if it is an ActorShell.
 */
class ActorShell(h: Handler) extends HandlerActor {
  override def !(msg: AnyRef): Unit = {
    Message.obtain(h, 0, msg).sendToTarget()
  }

  override def tell(msg: AnyRef): Unit = {
    Message.obtain(h, 0, msg).sendToTarget()
  }

  override def ?(msg: AnyRef)(implicit requester: Handler): Unit = {
    Message.obtain(h, 0, (requester, msg)).sendToTarget()
  }

  override def request(msg: AnyRef)(implicit requester: Handler): Unit = {
    Message.obtain(h, 0, (requester, msg)).sendToTarget()
  }
}

/**
 * Mixin to provide the Erlang-inspired Actor DSL to Android's Handler.
 *
 * Mix this trait into a class, or extends the ChattyFragment/ChattyActivity
 * trait for a more complete package.
 */
trait ActorConversion {
  implicit def handlerToActor(h: Handler): HandlerActor = new ActorShell(h)
}

/**
 * Mixin for Fragments that want to communicate over the event bus
 *
 * This trait automatically registers and unregisters the Fragment from
 * the global event bus according to the Fragment lifecycle. It also
 * provide an implicit HandlerActor for use with `HandlerActor::request`,
 * as well as the `ActorConversion` mixin
 *
 * The Fragment that wants to mix this trait in must implement Handler.Callback
 * to handle incoming messages, and AppService.FragmentInjection for access
 * to the Global EventHub
 *
 * Created by athran on 5/8/14.
 */
trait ChattyFragment extends Fragment with ActorConversion {
  self: Handler.Callback with AppService.FragmentInjection =>

  implicit lazy val communicator = new Handler(this)

  override def onStart() {
    super.onStart()
    app.eventHub ! EventHub.Subscribe(communicator)
  }

  override def onStop() {
    super.onStop()
    app.eventHub ! EventHub.Unsubscribe(communicator)
  }

}

/**
 * Same as ChattyFrag, but for Activity.
 */
trait ChattyActivity extends Activity with ActorConversion {
  self: Handler.Callback with AppService.ActivityInjection =>

  implicit lazy val communicator = new Handler(this)

  override def onStart() {
    super.onStart()
    app.eventHub ! EventHub.Subscribe(communicator)
  }

  override def onStop() {
    super.onStop()
    app.eventHub ! EventHub.Unsubscribe(communicator)
  }

}

abstract class Server extends Handler.Callback {

  def init(ctx: Context): Unit

  def handleRequest(req: AnyRef): Unit

  var requester: HandlerActor = null

  override def handleMessage(msg: Message): Boolean = {
    msg.obj match {
      case Initialize(ctx) => init(ctx)
      case (r: HandlerActor, req: AnyRef) =>
        requester = r
        handleRequest(req)
    }
    true
  }
}

private[service] case class Initialize(ctx: Context)