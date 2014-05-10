package edu.vanderbilt.doreguide

import android.os.{Message, Handler, Bundle}
import android.app.{FragmentTransaction, Fragment, Activity}
import android.view.{MenuItem, Menu}

import edu.vanderbilt.doreguide.service.{ActorConversion, EventHub, AppService}
import edu.vanderbilt.doreguide.MainActivity._
import edu.vanderbilt.doreguide.model.Place

class MainActivity extends Activity
                           with AppService.ActivityInjection
                           with Handler.Callback
                           with ActorConversion
{
  lazy val communicator = new Handler(this)
  implicit val implicitMain = this
  var mainState: MainState = null

  override def onCreate(saved: Bundle) {
    super.onCreate(saved)
    setContentView(R.layout.activity_main)
    getActionBar.setDisplayHomeAsUpEnabled(true)
  }

  override def onStart() {
    super.onStart()
    app.eventbus ! EventHub.Subscribe(communicator)
    ViewingCurrentLocation.enter
  }

  override def onStop() {
    super.onStop()
    mainState.leave
    app.eventbus ! EventHub.Unsubscribe(communicator)
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
        mainState.leave
        ViewingHearted.enter
        true

      case android.R.id.home =>
        mainState.leave
        ViewingCurrentLocation.enter
        true

      case _ => super.onOptionsItemSelected(item)
    }
  }

  override def onBackPressed() {
    mainState.handleEvent(BackButton)
  }

  override def handleMessage(msg: Message): Boolean = {
    mainState.handleEvent(msg.obj)
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
        case PlaceDetailFrag.NearbyPlaceSelected(plc) =>
          this.leave
          ViewingPlaceFromCurrent.enter
          ViewingPlaceFromCurrent.handleEvent(plc)
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
                       add(R.id.main_underbar, fragUnder))
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
