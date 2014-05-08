package edu.vanderbilt.doreguide.view

import android.app.{Activity, Fragment}
import edu.vanderbilt.doreguide.service.{EventBus, HandlerActor}
import android.os.Handler

/**
 * Mixin for Fragments that want to communicate over the event bus
 *
 * This trait automatically registers and unregisters the Fragment from
 * the global event bus according to the Fragment lifecycle. It also
 * provide an implicit HandlerActor for use with `HandlerActor::request`.
 *
 * Created by athran on 5/8/14.
 */
trait ChattyFrag extends Fragment {
  self: Handler.Callback with SimpleInjections.FragmentInjection =>

  implicit lazy val communicator = HandlerActor.sync(this)

  override def onStart() {
    super.onStart()
    dore.eventbus ! EventBus.Subscribe(communicator)
  }

  override def onStop() {
    super.onStop()
    dore.eventbus ! EventBus.Unsubscribe(communicator)
  }

}

trait ChattyActivity extends Activity {
  self: Handler.Callback with SimpleInjections.ActivityInjection =>

  implicit lazy val communicator = HandlerActor.sync(this)

  override def onStart() {
    super.onStart()
    dore.eventbus ! EventBus.Subscribe(communicator)
  }

  override def onStop() {
    super.onStop()
    dore.eventbus ! EventBus.Unsubscribe(communicator)
  }

}
