package edu.vanderbilt.doreguide.service

import android.os.{Message, Handler}

/**
 * Created by athran on 4/19/14.
 */
object FeedbackServer {

  case class Comment(email: String, body: String)

}

private[service] class FeedbackServer extends Handler.Callback {

  def handleMessage(msg: Message): Boolean = {
    true
  }

}
