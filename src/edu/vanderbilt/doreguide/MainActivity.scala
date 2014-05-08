package edu.vanderbilt.doreguide

import android.os.{Message, Handler, Bundle}
import android.app.{FragmentManager, Fragment, Activity}
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
    getActionBar.setDisplayHomeAsUpEnabled(true)
  }

  override def onStart() {
    super.onStart()
    dore.eventbus ! EventBus.Subscribe(communicator)
    getFragmentManager.
        beginTransaction().
        replace(R.id.main_main,
              PlaceDetailFrag.showNearestPlace,
              "PlaceDetailFrag").
        commit()
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
    item.getItemId match {
      case R.id.action_settings =>
        true

      case R.id.action_hearted =>
        getFragmentManager.
            beginTransaction().
            addToBackStack(null).
            replace(R.id.main_main, new HeartFrag, "HeartFrag").
            commit()
        true

      case android.R.id.home =>
          getFragmentManager.popBackStack(null,
                                           FragmentManager.POP_BACK_STACK_INCLUSIVE)
        true

      case _ => super.onOptionsItemSelected(item)
    }
  }

  override def handleMessage(msg: Message): Boolean = {
    msg.obj match {
      case HeartFrag.MapButtonClicked =>
        getFragmentManager.
            beginTransaction().
            addToBackStack(null).
            replace(R.id.main_main,
                    PlacesMapFragment.showHearted).
            commit()

      case PlaceDetailFrag.MapButtonClicked(plc) =>
        getFragmentManager.
            beginTransaction().
            addToBackStack(null).
            replace(R.id.main_main, null).
            commit()

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
