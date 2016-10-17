package models.song

import concurrent.duration.Duration

case class Song(id: Int, name: String, artist: String, length: Duration, location: String)