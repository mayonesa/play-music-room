package models.song

import concurrent.duration._

case class Song(id: Int, name: String, artist: String, length: Duration, location: String) {
  lazy val timeStr = getTimeStr
  private def getTimeStr = {
    val mins = length.toMinutes
    f"$mins%01d:${(length.toSeconds - Duration(mins, MINUTES).toSeconds)}%02d"
  }
}
