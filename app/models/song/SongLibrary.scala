package models.song

import concurrent.{ Future, blocking }
import concurrent.duration._
import javax.inject.{ Inject, Singleton }
import java.io.File
import play.api.Logger
import akka.actor.ActorSystem

@Singleton
class SongLibrary @Inject() (system: ActorSystem) {
  def apply(songId: Int): Song = songs(songId)
  private val logger = Logger(getClass)
  private val ioOps = system.dispatchers.lookup("contexts.io-ops")
  private val commonPath = "/Users/john_jimenez/Music/music-room/"

  // TODO: 
  // biggest lie
  // something in the way she moves me
  // beach boys
  // ella fitzegerald - dancing cheek-to-cheek
  private val songs = Map((1 -> Song(1, "Big in Japan", "Tom Waits", 245 seconds, commonPath + "Tom_Waits-Big_In_Japan.mp3")),
    (2 -> Song(2, "Someone Great", "LCD Soundsystem", 386 seconds, commonPath + "LCD_Soundsystem-Someone_Great.mp3")),
    (3 -> Song(3, "Feeling Good", "Nina Simone", 175 seconds, commonPath + "Nina_Simone-Feeling_Good.mp3")),
    (4 -> Song(4, "Nabucco: Chorus of the Hebrew Slaves", "London Phil Orch", 299 seconds, commonPath + "Nabucco_Chorus_Hebrew_Slaves.mp3")))
  def allSongs: Iterable[Song] = songs.values
  def getFile(songId: Int): Future[File] = Future {
    blocking {
      new File(songs(songId).location)
    }
  }(ioOps)
}