package models.song

import concurrent.duration.Duration

case class Song(id: Int, name: String, author: String, length: Duration, location: String)