package edu.vanderbilt.doreguide.service

import android.os.{Looper, Message, Handler}
import android.content.Context
import edu.vanderbilt.doreguide.service.Dore.Initialize

/**
 * Enhanced Handler that supports the standard Actor interface.
 *
 * Created by athran on 4/15/14.
 */
trait HandlerActor extends Handler {

  def !(msg: AnyRef): Unit = {
    Message.obtain(this, 0, msg).sendToTarget()
  }

  def request(msg: AnyRef)(implicit requester: HandlerActor): Unit = {
    this ! (requester, msg)
  }

}

object HandlerActor {

  /**
   * Create a synchronous Handler that runs tasks on the main thread
   */
  def sync(callback: Handler.Callback): HandlerActor = {
    new SyncHandlerActor(callback)
  }

  /**
   * Create an asynchronous Handler that runs tasks on a separate HandlerThread,
   * one looper is passed in as parameter.
   */
  def async(looper: Looper,
            callback: Handler.Callback): HandlerActor = {
    new AsyncHandlerActor(looper, callback)
  }

  private class AsyncHandlerActor(looper: Looper,
                                  callback: Handler.Callback)
      extends Handler(looper, callback)
              with HandlerActor

  private class SyncHandlerActor(callback: Handler.Callback)
      extends Handler(callback)
              with HandlerActor

  trait ImplicitRequester {

    implicit val requester: HandlerActor

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

}

