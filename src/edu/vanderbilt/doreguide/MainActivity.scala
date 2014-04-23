package edu.vanderbilt.doreguide

import android.os.{Message, Handler, Bundle}
import android.app.{Fragment, Activity}
import edu.vanderbilt.doreguide.service.{HandlerActor, EventBus}
import android.view.{MenuItem, Menu}

class MainActivity extends Activity
                           with SimpleInjections.ActivityInjection
                           with Handler.Callback {

  lazy val communicator = new HandlerActor(this.getMainLooper, this)
  var currentFragment: Fragment = null

	override def onCreate(saved: Bundle) {
		super.onCreate(saved)
		setContentView(R.layout.activity_main)
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
  /*
  @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.stop, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            AboutsActivity.open(this);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }
   */

  def handleMessage(msg: Message): Boolean = {
    msg.obj match {
      case PlaceDetailFrag.MapButtonClicked(plc) =>
      case _ =>
    }
    true
  }

}
