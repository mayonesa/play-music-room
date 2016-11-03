package models.song

import concurrent.duration._

case class Song(id: Int, name: String, artist: String, dur: Duration, location: String) {
  lazy val timeStr = getTimeStr
  private def getTimeStr = {
    val mins = dur.toMinutes
    f"$mins%01d:${(dur.toSeconds - Duration(mins, MINUTES).toSeconds)}%02d"
  }
}