package edu.vanderbilt.doreguide.service

import android.os.{Message, Handler}
import android.graphics.Bitmap
import android.content.Context
import com.nostra13.universalimageloader.core.{ImageLoaderConfiguration, DisplayImageOptions, ImageLoader}
import java.io.InputStreamReader
import java.net.URL
import com.google.gson.JsonParser

/**
 * Downloads and caches image on request
 *
 * Created by athran on 4/15/14.
 */
object ImageServer {

  /**
   * Downloads or fetches the image from the URL.
   *
   * Reply: Image
   */
  case class DispatchImage(url: String)

  /**
   * Downloads or fetches the image from the asset ID
   * in our database.
   *
   * See: https://github.com/AliceCengal/vanderbilt-data/blob/master/images.json
   *
   * Reply: Image
   */
  case class DispatchImageFromId(id: Int)

  /**
   * Container for image reply
   */
  case class Image(url: String, img: Bitmap)

  val rawUrl = "https://raw2.github.com/AliceCengal/vanderbilt-data/master/images.json"
}

private[service] class ImageServer extends Handler.Callback {

  import Dore._
  import ImageServer._
  import scala.collection.JavaConverters._

  private var imageLoader: ImageLoader = null
  private var imageBank: Map[Int,String] = Map.empty

  def handleMessage(msg: Message): Boolean = {
    msg.obj match {
      case Initialize(ctx)                            => init(ctx)
      case (r: HandlerActor, DispatchImage(url))      => dispatchImage(r, url)
      case (r: HandlerActor, DispatchImageFromId(id)) => dispatchImage(r, imageBank(id))
    }
    true
  }

  private def init(ctx: Context): Unit = {
    initImageBank()
    initImageLoader(ctx)
  }

  private def initImageBank(): Unit = {
    this.imageBank =
        new JsonParser()
            .parse(
              new InputStreamReader(
                new URL(rawUrl)
                    .openConnection()
                    .getInputStream))
            .getAsJsonArray
            .iterator().asScala
            .map(_.getAsJsonObject)
            .map(obj =>
                (obj.get("id").getAsInt,
                 obj.get("url").getAsString))
            .toMap
  }

  private def initImageLoader(ctx: Context): Unit = {
    if (imageLoader == null) {
      this.imageLoader = ImageLoader.getInstance()
      val config =
        new ImageLoaderConfiguration.Builder(ctx)
            .defaultDisplayImageOptions(
              new DisplayImageOptions.Builder()
                  .cacheInMemory(true)
                  .cacheOnDisc(true)
                  .build())
            .build()
      this.imageLoader.init(config)
    }
  }

  private def dispatchImage(requester: HandlerActor,
                            url:       String): Unit = {
    requester ! Image(url, imageLoader.loadImageSync(url))
  }

}
