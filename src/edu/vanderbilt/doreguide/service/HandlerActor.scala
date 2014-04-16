package edu.vanderbilt.doreguide.service

import android.os.{Looper, Message, Handler}

/**
 * Enhanced Handler that supports the standard Actor interface.
 *
 * Created by athran on 4/15/14.
 */
class HandlerActor(looper: Looper, callback: Handler.Callback)
    extends Handler(looper, callback) {

  def !(msg: AnyRef): Unit = {
    Message
        .obtain(this, 0, msg)
        .sendToTarget()
  }

}
