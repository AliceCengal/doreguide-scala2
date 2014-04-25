package edu.vanderbilt.doreguide

import android.os.{Message, Handler, Bundle}
import android.app.{Fragment, Activity}
import android.view.{MenuItem, Menu}

import edu.vanderbilt.doreguide.service.{HandlerActor, EventBus}
import edu.vanderbilt.doreguide.view.SimpleInjections

class MainActivity extends Activity
                           with SimpleInjections.ActivityInjection
                           with Handler.Callback {

  lazy val communicator = HandlerActor.sync(this)
  var currentFragment: Fragment = null

	override def onCreate(saved: Bundle) {
		super.onCreate(saved)
		setContentView(R.layout.activity_main)
    getFragmentManager.
        beginTransaction().
        add(R.id.main_main,
            PlaceDetailFrag.showNearestPlace,
            "PlaceDetailFrag").
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

  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    getMenuInflater.inflate(R.menu.main, menu)
    true
  }

  override def onOptionsItemSelected(item: MenuItem): Boolean = {
    if (item.getItemId == R.id.action_settings) {
      true
    } else if (item.getItemId == R.id.action_hearted) {
      getFragmentManager.
          beginTransaction().
          replace(R.id.main_main, new HeartFrag, "HeartFrag").
          commit()
      true
    } else {
      super.onOptionsItemSelected(item)
    }
  }

  def handleMessage(msg: Message): Boolean = {
    msg.obj match {
      case HeartFrag.MapButtonClicked =>
        getFragmentManager.
        beginTransaction().
        addToBackStack(null).
        replace(R.id.main_main,
                PlacesMapFragment.showHearted).
        commit()
      case PlaceDetailFrag.MapButtonClicked(plc) =>
      case PlaceDetailFrag.NearbyPlaceSelected(plc) =>
        getFragmentManager.
        beginTransaction().
        replace(R.id.main_main,
                PlaceDetailFrag.showThisPlace(plc)).
        commit()
      case _ =>
    }
    true
  }

}
