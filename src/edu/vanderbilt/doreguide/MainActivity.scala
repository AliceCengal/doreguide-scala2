package edu.vanderbilt.doreguide

import android.os.{Message, Handler, Bundle}
import android.app.{FragmentTransaction, FragmentManager, Fragment, Activity}
import android.view.{MenuItem, Menu}

import edu.vanderbilt.doreguide.service.{HandlerActor, EventBus}
import edu.vanderbilt.doreguide.view.SimpleInjections
import edu.vanderbilt.doreguide.MainActivity._
import edu.vanderbilt.doreguide.model.Place

class MainActivity extends Activity
                           with SimpleInjections.ActivityInjection
                           with Handler.Callback {

  lazy val communicator = HandlerActor.sync(this)
  //var currentFragment: Fragment = null
  implicit val implicitMain = this
  var mainState: MainState = null

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
            replace(R.id.main_underbar,
                    new MapUnderbarFrag).
            commit()

      case PlaceDetailFrag.MapButtonClicked(plc) =>
        getFragmentManager.
            beginTransaction().
            addToBackStack(null).
            replace(R.id.main_main,
                    PlacesMapFragment.showAll).
            replace(R.id.main_underbar,
                    new MapUnderbarFrag).
            commit()

      case PlaceDetailFrag.NearbyPlaceSelected(plc) =>
        getFragmentManager.
            beginTransaction().
            replace(R.id.main_main,
                    PlaceDetailFrag.showThisPlace(plc)).
            commit()

      case HeartFrag.ListItemClicked(plc) =>
        getFragmentManager.
            beginTransaction().
            addToBackStack(null).
            replace(R.id.main_main,
                    PlaceDetailFrag.showThisPlace(plc)).
            commit()

      case MapUnderbarFrag.MapUnderbarClicked(plc) =>
        getFragmentManager.
            beginTransaction().
            addToBackStack(null).
            replace(R.id.main_main,
                    PlaceDetailFrag.showThisPlace(plc)).
            remove(getFragmentManager.findFragmentById(R.id.main_underbar)).
            commit()

      case _ =>
    }
    true
  }

  def clearFragments() {
    getFragmentManager.
        beginTransaction().
        remove(getFragmentManager.findFragmentById(R.id.main_main)).
        remove(getFragmentManager.findFragmentById(R.id.main_underbar)).
        commit()
  }

  def superBack() {
    super.onBackPressed()
  }

  def transaction(action: FragmentTransaction => Unit) {
    val ft = getFragmentManager.beginTransaction()
    action(ft)
    ft.commit()
  }

}

object MainActivity {

  case object HeartMenuItem
  case object BackButton
  case object HomeUp
  case object SettingMenuItem

  trait MainState {
    def enter(implicit main: MainActivity)
    def handleEvent(event: AnyRef)(implicit main: MainActivity)
    def leave(implicit main: MainActivity)
  }

  object ViewingCurrentLocation extends MainState {

    private var frag: Fragment = null

    def enter(implicit main: MainActivity) {
      main.mainState = this
      frag = PlaceDetailFrag.showNearestPlace
      main.transaction(_.add(R.id.main_main, frag))
    }

    def handleEvent(event: AnyRef)(implicit main: MainActivity): Unit = {
      event match {
        case BackButton =>
          frag = null
          main.superBack()
        case HeartMenuItem =>
          this.leave
          ViewingHearted.enter
        case PlaceDetailFrag.MapButtonClicked(plc) =>
          this.leave
          ViewingMap.enter
        case _ =>
      }
    }

    def leave(implicit main: MainActivity): Unit = {
      if (frag != null) {
        main.transaction(_.remove(frag))
        frag = null
      }
    }
  }

  object ViewingHearted extends MainState {
    private var frag: Fragment = null

    def enter(implicit main: MainActivity): Unit = {
      main.mainState = this
      frag = new HeartFrag
      main.transaction(_.add(R.id.main_main, frag))
    }

    def handleEvent(event: AnyRef)(implicit main: MainActivity): Unit = {
      event match {
        case BackButton =>
          this.leave
          ViewingCurrentLocation.enter
        case HeartFrag.MapButtonClicked =>
          this.leave
          ViewingHeartedMap.enter
        case HeartFrag.ListItemClicked(plc) =>
          this.leave
          ViewingPlaceFromHearted.enter
          ViewingPlaceFromHearted.handleEvent(plc)
        case _          =>
      }
    }

    def leave(implicit main: MainActivity): Unit = {
      if (frag != null) {
        main.transaction(_.remove(frag))
        frag = null
      }
    }
  }

  object ViewingMap extends MainState {
    private var frag: Fragment = null
    private var fragUnder: Fragment = null

    def enter(implicit main: MainActivity): Unit = {
      main.mainState = this
      frag = PlacesMapFragment.showAll
      fragUnder = new MapUnderbarFrag
      main.transaction(_.
                       add(R.id.main_main, frag).
                       add(R.id.main_underbar, fragUnder))
    }

    def handleEvent(event: AnyRef)(implicit main: MainActivity): Unit = {
      event match {
        case BackButton =>
          this.leave
          ViewingCurrentLocation.enter
        case MapUnderbarFrag.MapUnderbarClicked(plc) =>
          this.leave
          ViewingPlaceFromMap.enter
          ViewingPlaceFromMap.handleEvent(plc)
        case _ =>
      }
    }

    def leave(implicit main: MainActivity): Unit = {
      main.transaction{ ft =>
        if (frag != null) {
          ft.remove(frag)
          frag = null
        }
        if (fragUnder != null) {
          ft.remove(fragUnder)
          fragUnder = null
        }
      }
    }
  }

  object ViewingPlaceFromMap extends MainState {
    private var frag: Fragment = null

    def enter(implicit main: MainActivity): Unit = {
      main.mainState = this
    }

    def handleEvent(event: AnyRef)(implicit main: MainActivity): Unit = {
      event match {
        case plc: Place =>
          frag = PlaceDetailFrag.showThisPlace(plc)
          main.transaction(_.add(R.id.main_main, frag))
        case PlaceDetailFrag.NearbyPlaceSelected(plc) =>
          frag = PlaceDetailFrag.showThisPlace(plc)
          main.transaction(_.replace(R.id.main_main, frag))
        case BackButton | PlaceDetailFrag.MapButtonClicked(_) =>
          this.leave
          ViewingMap.enter
        case _ =>
      }
    }

    def leave(implicit main: MainActivity): Unit = {
      if (frag != null) {
        main.transaction(_.remove(frag))
        frag = null
      }
    }
  }

  object ViewingPlaceFromCurrent extends MainState {
    private var frag: Fragment = null

    def enter(implicit main: MainActivity): Unit = {
      main.mainState = this
    }

    def handleEvent(event: AnyRef)(implicit main: MainActivity): Unit = {
      event match {
        case plc: Place =>
          frag = PlaceDetailFrag.showThisPlace(plc)
          main.transaction(_.add(R.id.main_main, frag))
        case PlaceDetailFrag.NearbyPlaceSelected(plc) =>
          frag = PlaceDetailFrag.showThisPlace(plc)
          main.transaction(_.replace(R.id.main_main, frag))
        case PlaceDetailFrag.MapButtonClicked(_) =>
          this.leave
          ViewingMap.enter
        case BackButton =>
          this.leave
          ViewingCurrentLocation.enter
        case _ =>
      }
    }

    def leave(implicit main: MainActivity): Unit = {
      if (frag != null) {
        main.transaction(_.remove(frag))
        frag = null
      }
    }
  }

  object ViewingHeartedMap extends MainState {
    private var frag: Fragment = null
    private var fragUnder: Fragment = null

    def enter(implicit main: MainActivity): Unit = {
      main.mainState = this
      frag = PlacesMapFragment.showHearted
      fragUnder = new MapUnderbarFrag
      main.transaction(_.
                       add(R.id.main_main, frag).
                       add(R.id.main_underbar, frag))
    }

    def handleEvent(event: AnyRef)(implicit main: MainActivity): Unit = {
      event match {
        case MapUnderbarFrag.MapUnderbarClicked(plc) =>
          this.leave
          ViewingPlaceFromHeartedMap.enter
          ViewingPlaceFromHeartedMap.handleEvent(plc)
        case BackButton =>
          this.leave
          ViewingHearted.enter
        case _ =>
      }
    }

    def leave(implicit main: MainActivity): Unit = {
      main.transaction{ ft =>
        if (frag != null) ft.remove(frag)
        if (fragUnder != null) ft.remove(fragUnder)

        frag = null
        fragUnder = null
      }
    }
  }

  object ViewingPlaceFromHeartedMap extends MainState {
    private var frag: Fragment = null

    def enter(implicit main: MainActivity): Unit = {
      main.mainState = this
    }

    def handleEvent(event: AnyRef)(implicit main: MainActivity): Unit = {
      event match {
        case plc: Place =>
          frag = PlaceDetailFrag.showThisPlace(plc)
          main.transaction(_.add(R.id.main_main, frag))
        case PlaceDetailFrag.NearbyPlaceSelected(plc) =>
          frag = PlaceDetailFrag.showThisPlace(plc)
          main.transaction(_.replace(R.id.main_main, frag))
        case BackButton | PlaceDetailFrag.MapButtonClicked(_) =>
          this.leave
          ViewingHeartedMap.enter
        case _ =>
      }
    }

    def leave(implicit main: MainActivity): Unit = {
      if (frag != null) {
        main.transaction(_.remove(frag))
        frag = null
      }
    }
  }

  object ViewingPlaceFromHearted extends MainState {
    private var frag: Fragment = null

    def enter(implicit main: MainActivity): Unit = {
      main.mainState = this
    }

    def handleEvent(event: AnyRef)(implicit main: MainActivity): Unit = {
      event match {
        case plc: Place =>
          frag = PlaceDetailFrag.showThisPlace(plc)
          main.transaction(_.add(R.id.main_main, frag))
        case PlaceDetailFrag.NearbyPlaceSelected(plc) =>
          frag = PlaceDetailFrag.showThisPlace(plc)
          main.transaction(_.replace(R.id.main_main, frag))
        case BackButton =>
          this.leave
          ViewingHearted.enter
        case PlaceDetailFrag.MapButtonClicked(_) =>
          this.leave
          ViewingHeartedMap.enter
        case _ =>
      }
    }

    def leave(implicit main: MainActivity): Unit = {
      if (frag != null) {
        main.transaction(_.remove(frag))
        frag = null
      }
    }
  }

}
