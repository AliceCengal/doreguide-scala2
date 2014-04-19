package edu.vanderbilt.doreguide

import android.os.{Message, Handler, Bundle}
import android.app.{Fragment, Activity}
import edu.vanderbilt.doreguide.service.{HandlerActor, EventBus}

class MainActivity extends Activity
                           with SimpleInjections.ActivityInjection
                           with Handler.Callback {

  lazy val communicator = new HandlerActor(this.getMainLooper, this)

	override def onCreate(saved: Bundle) {
		super.onCreate(saved)
		setContentView(R.layout.activity_stop)
    getFragmentManager.
        beginTransaction().
        add(R.id.main_main, new PlaceDetailFrag, "PlaceDetailFrag").
        commit()
	}

  override def onStart() {
    super.onStart()
    dore.eventbus ! EventBus.Subscribe(communicator)
  }

  override def onStop() {
    super.onStop()
    dore.eventbus ! EventBus.Unsubscribe(communicator)
  }

  def handleMessage(msg: Message): Boolean = true

  trait FragmentSpawner {
    def spawn: Fragment
  }

}
