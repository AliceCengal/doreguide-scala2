package edu.vanderbilt.doreguide

import android.app.Fragment
import edu.vanderbilt.doreguide.service.HandlerActor
import android.os.{Message, Handler}
import android.widget.{ImageButton, AdapterView, ListView}
import android.view.View
import edu.vanderbilt.doreguide.ArrayAdapterBuilder.ToString
import edu.vanderbilt.doreguide.model.Place
import edu.vanderbilt.doreguide.service.EventBus.{Unsubscribe, Subscribe}

/**
 * A page showing all hearted Places
 *
 * Created by athran on 4/19/14.
 */
class HeartFrag extends Fragment
                        with SimpleInjections.FragmentInjection
                        with FragmentViewUtil
                        with Handler.Callback
                        with AdapterView.OnItemClickListener
                        with View.OnClickListener {

  def layoutId = R.layout.heart_list

  lazy val controller = new HandlerActor(this.getActivity.getMainLooper, this)

  def list = component[ListView](R.id.lv_heart)
  def btnMap = component[ImageButton](R.id.btn_map)

  def handleMessage(msg: Message): Boolean = {
    true
  }

  override def onStart() {
    super.onStart()

    import scala.collection.JavaConversions.seqAsJavaList

    list.setAdapter(ArrayAdapterBuilder.
                    fromCollection(dore.getAllHearted).
                    withContext(getActivity).
                    withResource(R.layout.simple_text).
                    withStringer(new ToString[Place] {
                      def apply(plc: Place): String = plc.name
                    }).
                    build())

    list.setOnItemClickListener(this)
    btnMap.setOnClickListener(this)

    dore.eventbus ! Subscribe(controller)
  }

  override def onStop() {
    super.onStop()
    dore.eventbus ! Unsubscribe(controller)
  }

  def onItemClick(p1: AdapterView[_],
                  p2: View,
                  position: Int,
                  p4: Long): Unit = {
    dore.eventbus ! HeartFrag.ListItemClicked(dore.getAllHearted(position))
  }

  def onClick(p1: View): Unit = {
    dore.eventbus ! HeartFrag.MapButtonClicked
  }
}

object HeartFrag {

  case class ListItemClicked(plc: Place)

  case object MapButtonClicked

}