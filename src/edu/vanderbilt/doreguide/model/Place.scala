package edu.vanderbilt.doreguide.model

import com.google.gson.{JsonParser, JsonObject}
import java.io.Reader

/**
 * Created by athran on 4/15/14.
 */
case class Place(uniqueId:    Int,
                 name:        String,
                 description: String,
                 latitude:    Double,
                 longitude:   Double,
                 images:      List[Int],
                 hours:       String,
                 categories:  List[PlaceCategory])

object Place {

  import scala.collection.JavaConverters._

  val DEFAULT_ID    = -1
  val MAX_PLACE_ID  = 9999 // Largest Id allowed for a place

  val TAG_ID        = "id"
  val TAG_NAME      = "name"
  val TAG_DESC      = "placeDescription"
  val TAG_CAT       = "category"
  val TAG_HOURS     = "hours"
  val TAG_IMAGE     = "imagePath"
  val TAG_AUDIO     = "audioPath"
  val TAG_VIDEO     = "videoPath"
  val TAG_LAT       = "latitude"
  val TAG_LON       = "longitude"
  val TAG_IMAGEIDS  = "imageIds"

  def fromJsonObject(obj: JsonObject): Place = {
    Place(
      obj.get(TAG_ID).getAsInt,
      obj.get(TAG_NAME).getAsString,
      obj.get(TAG_DESC).getAsString,
      obj.get(TAG_LAT).getAsDouble,
      obj.get(TAG_LON).getAsDouble,
      obj.get(TAG_IMAGEIDS).getAsJsonArray
          .iterator()
          .asScala
          .map(_.getAsInt)
          .toList,
      obj.get(TAG_HOURS).getAsString,
      obj.get(TAG_CAT).getAsJsonArray
          .iterator()
          .asScala
          .map(_.getAsString)
          .map(PlaceCategory.fromName)
          .toList
    )
  }

  def fromReader(reader: Reader): Place = {
    fromJsonObject(
      new JsonParser()
          .parse(reader)
          .getAsJsonObject)
  }

}