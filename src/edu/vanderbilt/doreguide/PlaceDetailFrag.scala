package edu.vanderbilt.doreguide

import android.view.{View, LayoutInflater}
import android.os.{Message, Handler}
import android.app.Fragment
import android.location.Location
import android.widget.{Toast, ImageButton, ScrollView, LinearLayout, EditText, Button, ImageView, TextView}
import android.graphics.Bitmap
import android.view.View.OnClickListener

import edu.vanderbilt.doreguide.model.Place
import edu.vanderbilt.doreguide.view.{SimpleInjections, FragmentViewUtil}
import edu.vanderbilt.doreguide.service.{PlaceServer, Geomancer}

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

  self: PlaceDetailFrag.DetailBehaviour =>

  import PlaceDetailFrag._
  import service._

  lazy val controller = new HandlerActor(this.getActivity.getMainLooper, this)
  var place: Place = null

  def tvTitle       = component[TextView](R.id.tv_title)
  def ivMainImage   = component[ImageView](R.id.iv1)
  def tvDescription = component[TextView](R.id.tv_desc)
  def llNearby      = component[LinearLayout](R.id.ll_nearby)

  def btnMap        = component[ImageButton](R.id.btn_map)
  def btnHeart      = component[ImageButton](R.id.btn_heart)

  def btnSubmit     = component[Button](R.id.btn_submit)
  def etEmail       = component[EditText](R.id.et_email)
  def etContent     = component[EditText](R.id.et_content)

  override def layoutId = R.layout.place_detail

  override def onStart() {
    super.onStart()

    btnSubmit.setOnClickListener(this)
    ivMainImage.setOnClickListener(this)
    btnHeart.setOnClickListener(this)
    btnMap.setOnClickListener(this)

    dore.eventbus ! EventBus.Subscribe(controller)
    init()
  }

  override def onStop() {
    super.onStop()
    dore.eventbus ! EventBus.Unsubscribe(controller)
  }

  def handleMessage(msg: Message): Boolean = {
    msg.obj match {
      case Geomancer.LocationResult(maybeLoc) => handleLoc(maybeLoc)
      case PlaceServer.PlaceResult(plcs)      => handlePlaces(plcs)
      case ImageServer.Image(url, img)        => setMainImage(img)
      case _                                  =>
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
    if (plcs.size == 1) {
      val plc = plcs(0)
      dore.placeServer !
      (controller, PlaceServer.FindNClosest(plc.latitude,
                                            plc.longitude,
                                            NEARBY_COUNT + 1))
    } else {
      displayPlace(plcs(0))
      displayNearbyPlaces(plcs.tail)
      component[ScrollView](R.id.sv_place_detail_parent).smoothScrollTo(0,0)
    }
  }

  private def displayPlace(plc: Place) {
    this.place = plc

    tvTitle.setText(plc.name)
    tvTitle.setVisibility(View.VISIBLE)
    tvDescription.setText(plc.description)
    tvDescription.setVisibility(View.VISIBLE)

    if (dore.isHearted(plc)) {
      btnHeart.setImageResource(R.drawable.rating_not_important)
    } else {
      btnHeart.setImageResource(R.drawable.rating_important)
    }

    if (!plc.images.isEmpty) {
      dore.imageServer ! (controller, ImageServer.DispatchImageFromId(plc.images(0)))
    }
  }

  private def displayNearbyPlaces(plcs: List[Place]) {
    while (llNearby.getChildCount > 1) {
      llNearby.removeViewAt(1)
    }

    plcs.foreach { plc =>
      val tv = LayoutInflater.
               from(getActivity).
               inflate(R.layout.simple_text, null).
               asInstanceOf[TextView]
      tv.setText(plc.name)
      tv.setOnClickListener(createNearbyListDirective(plc))

      llNearby.addView(tv)}
  }

  private def setMainImage(img: Bitmap) {
    ivMainImage.setImageBitmap(img)
    controller.postDelayed(new Runnable(){
      def run(): Unit = {
        ivMainImage.setVisibility(View.VISIBLE)
      }
    }, 1000)
  }

  def onClick(v: View): Unit = {
    if (v == btnSubmit) {
      doCommentSend()

    } else if (v == btnHeart) {
      doHeartToggle()

    } else if (v == btnMap) {
      dore.eventbus ! MapButtonClicked(place)

    } else if (v == ivMainImage) {
      dore.eventbus ! MainImageClicked(place)
    }
  }

  private def doCommentSend() {
    val email = etEmail.getText.toString
    val body = etContent.getText.toString

    if (email == null || email.isEmpty) {
      showToast("Please put in your email address")
    } else if (body == null || body.isEmpty) {
      showToast("Please include your feedback")
    } else {
      dore.feedback ! FeedbackServer.Comment(email, body)
    }
  }

  private def doHeartToggle() {
    if (dore.isHearted(place)) {
      dore.unheart(place)
      btnHeart.setImageResource(R.drawable.rating_important)
    } else {
      dore.heart(place)
      btnHeart.setImageResource(R.drawable.rating_not_important)
    }
  }

  private def showToast(message: String) {
    Toast.makeText(this.getActivity, message, Toast.LENGTH_LONG).show()
  }

  private def createNearbyListDirective(plc: Place) = new OnClickListener {
    def onClick(p1: View): Unit = {
      dore.eventbus ! NearbyPlaceSelected(plc)
    }
  }

}

object PlaceDetailFrag {

  val NEARBY_COUNT = 10

  case class MapButtonClicked(plc: Place)

  case class MainImageClicked(plc: Place)

  case class NearbyPlaceSelected(nearby: Place)

  def showNearestPlace = {
    new PlaceDetailFrag with ShowNearest
  }

  def showThisPlace(plc: Place) = {
    new PlaceDetailFrag with ShowPlace {
      def placeId = plc.uniqueId
    }
  }

  trait DetailBehaviour {
    def init()
  }

  trait ShowNearest extends DetailBehaviour {
    self: PlaceDetailFrag =>

    def init() {
      dore.geomancer ! (controller, Geomancer.GetLocation)
    }

  }

  trait ShowPlace extends DetailBehaviour {
    self: PlaceDetailFrag =>

    def placeId: Int

    def init() {
      dore.placeServer ! (controller, PlaceServer.GetPlaceWithId(placeId))
    }
  }

}