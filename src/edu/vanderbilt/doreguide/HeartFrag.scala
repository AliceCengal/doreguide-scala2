package edu.vanderbilt.doreguide

import android.app.Fragment
import android.os.{Message, Handler}
import android.widget.{ImageButton, AdapterView, ListView}
import android.view.View

import edu.vanderbilt.doreguide.view.ArrayAdapterBuilder.ToString
import edu.vanderbilt.doreguide.model.Place
import edu.vanderbilt.doreguide.service.AppService.FragmentInjection
import edu.vanderbilt.doreguide.view.ArrayAdapterBuilder
import edu.vanderbilt.doreguide.service.ChattyFragment
import edu.vanderbilt.doreguide.service.Helpers.EasyFragment

/**
 * A page showing all hearted Places
 *
 * Created by athran on 4/19/14.
 */
class HeartFrag extends Fragment
                        with ChattyFragment
                        with FragmentInjection
                        with EasyFragment
                        with Handler.Callback
                        with AdapterView.OnItemClickListener
                        with View.OnClickListener
{
  def layoutId = R.layout.heart_list

  def list   = component[ListView](R.id.lv_heart)
  def btnMap = component[ImageButton](R.id.btn_map)

  def handleMessage(msg: Message): Boolean = {
    true
  }

  override def onStart() {
    super.onStart()

    import scala.collection.JavaConversions.seqAsJavaList

    list.setAdapter(ArrayAdapterBuilder.
                    fromCollection(app.getAllHearted).
                    withContext(getActivity).
                    withResource(R.layout.simple_text).
                    withStringer(new ToString[Place] {
                      def apply(plc: Place): String = plc.name
                    }).
                    build())

    list.setOnItemClickListener(this)
    btnMap.setOnClickListener(this)
  }

  def onItemClick(p1: AdapterView[_],
                  p2: View,
                  position: Int,
                  p4: Long): Unit = {
    app.eventbus ! HeartFrag.ListItemClicked(app.getAllHearted(position))
  }

  def onClick(p1: View): Unit = {
    app.eventbus ! HeartFrag.MapButtonClicked
  }
}

object HeartFrag {

  case class ListItemClicked(plc: Place)

  case object MapButtonClicked

}