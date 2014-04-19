package edu.vanderbilt.doreguide

import android.view.{View, ViewGroup, LayoutInflater}
import android.os.{Message, Handler, Bundle}
import android.app.Fragment
import edu.vanderbilt.doreguide.service.{ImageServer, PlaceServer, EventBus, HandlerActor}
import edu.vanderbilt.doreguide.service.Geomancer.{LocationResult, GetLocation}
import android.location.Location
import edu.vanderbilt.doreguide.service.PlaceServer.{FindNClosest, PlaceResult}
import edu.vanderbilt.doreguide.model.Place
import android.widget.{ScrollView, LinearLayout, EditText, Button, ImageView, TextView}
import android.graphics.Bitmap
import android.view.View.OnClickListener

/**
 * The page that displays the details of a Place
 *
 * Created by athran on 4/17/14.
 */
class PlaceDetailFrag extends Fragment
                              with SimpleInjections.FragmentInjection
                              with Handler.Callback
                              with FragmentViewUtil
                              with View.OnClickListener{

  val NEARBY_COUNT = 10

  lazy val controller = new HandlerActor(this.getActivity.getMainLooper, this)

  def tvTitle       = component[TextView](R.id.tv_title)
  def ivMainImage   = component[ImageView](R.id.iv1)
  def tvDescription = component[TextView](R.id.tv_desc)
  def llNearby      = component[LinearLayout](R.id.ll_nearby)

  def btnSubmit     = component[Button](R.id.btn_submit)
  def etEmail       = component[EditText](R.id.et_email)
  def etContent     = component[EditText](R.id.et_content)

  override def onCreateView(inflater:  LayoutInflater,
                            container: ViewGroup,
                            saved:     Bundle): View = {
    inflater.inflate(R.layout.place_detail, container, false)
  }

  override def onStart() {
    super.onStart()
    dore.eventbus ! EventBus.Subscribe(controller)
    dore.geomancer ! (controller, GetLocation)

    btnSubmit.setOnClickListener(this)
    ivMainImage.setOnClickListener(this)
  }

  override def onStop() {
    super.onStop()
    dore.eventbus ! EventBus.Unsubscribe(controller)
  }

  def handleMessage(msg: Message): Boolean = {
    msg.obj match {
      case LocationResult(maybeLoc)    => handleLoc(maybeLoc)
      case PlaceResult(plcs)           => handlePlaces(plcs)
      case ImageServer.Image(url, img) => setMainImage(img)
    }
    true
  }

  private def handleLoc(maybeLoc: Option[Location]) {

    import PlaceServer.FindNClosest

    for (loc <- maybeLoc) {
      dore.placeServer ! (controller, FindNClosest(loc.getLatitude,
                                                   loc.getLongitude,
                                                   NEARBY_COUNT + 1))
    }
  }

  private def handlePlaces(plcs: List[Place]) {

    displayPlace(plcs(0))

    while (llNearby.getChildCount > 1) {
      llNearby.removeViewAt(1)
    }

    plcs.tail.foreach { plc =>
      val tv = LayoutInflater.from(getActivity).inflate(R.layout.simple_text, null).asInstanceOf[TextView]
      tv.setText(plc.name)
      tv.setOnClickListener(new OnClickListener() {
        def onClick(p1: View): Unit = {
          dore.placeServer ! (controller, FindNClosest(plc.latitude, plc.longitude, NEARBY_COUNT + 1))
          component[ScrollView](R.id.sv_place_detail_parent).smoothScrollTo(0,0)
        }
      })

      llNearby.addView(tv)

    }

  }

  private def displayPlace(plc: Place) {
    tvTitle.setText(plc.name)
    tvDescription.setText(plc.description)

    dore.imageServer ! (controller, ImageServer.DispatchImageFromId(plc.images(0)))
  }

  private def setMainImage(img: Bitmap) {
    ivMainImage.setImageBitmap(img)
  }

  def onClick(v: View): Unit = {
    if (v == btnSubmit) {

    }
  }

}
