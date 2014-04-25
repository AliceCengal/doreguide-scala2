package edu.vanderbilt.doreguide.service

import android.content.Context

/**
 * Created by athran on 4/19/14.
 */
object FeedbackServer {

  case class Comment(email: String, body: String)

}

private[service] class FeedbackServer extends HandlerActor.Server {
  def init(ctx: Context): Unit = {}

  def handleRequest(req: AnyRef): Unit = {}
}
