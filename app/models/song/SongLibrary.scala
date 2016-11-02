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

  private val songs = Map((1 -> Song(1, "Big in Japan", "Tom Waits", 245 seconds, commonPath + "Tom_Waits-Big_In_Japan.mp3")),
    (2 -> Song(2, "Someone Great", "LCD Soundsystem", 386 seconds, commonPath + "LCD_Soundsystem-Someone_Great.mp3")),
    (3 -> Song(3, "Feeling Good", "Nina Simone", 175 seconds, commonPath + "Nina_Simone-Feeling_Good.mp3")),
    (10 -> Song(10, "Something in the Way She Moves", "The Beatles", 179 seconds, commonPath + "TheBeatlesSomethingInTheWaySheMoves.mp3")),
    (5 -> Song(5, "The Biggest Lie", "Elliott Smith", 158 seconds, commonPath + "ElliottSmith-TheBiggestLie.mp3")),
    (6 -> Song(6, "Cheek To Cheek", "Ella Fitzgerald & Louis Armstrong", 355 seconds, commonPath + "EllaFitzgerald&LouisArmstrong-CheekToCheek.mp3")),
    (7 -> Song(7, "Helplessness Blues", "Fleet Foxes", 304 seconds, commonPath + "FleetFoxes-HelplessnessBlues.mp3")),
    (8 -> Song(8, "Wouldn't It Be Nice", "Beach Boys", 153 seconds, commonPath + "BeachBoys-Wouldn'tItBeNice.mp3")),
    (11 -> Song(11, "The Funeral", "Band of Horses", 326 seconds, commonPath + "BandOfHorses-TheFuneral.mp3")),
    (12 -> Song(12, "Nabucco: Chorus of the Hebrew Slaves", "L.P.O.", 299 seconds, commonPath + "Nabucco_Chorus_Hebrew_Slaves.mp3")),
    (13 -> Song(13, "I'll Believe in Anything", "Wolf Parade", 275 seconds, commonPath + "WolfParade-I'llBelieveInAnything.mp3")),
    (14 -> Song(14, "On a Neck, on a Spit", "Grizzly Bear", 352 seconds, commonPath + "Grizzly_Bear-On_A_Neck_On_A_Spit.mp3")))
  def allSongs: Iterable[Song] = songs.values
  def getFile(songId: Int): Future[File] = Future {
    blocking {
      new File(songs(songId).location)
    }
  }(ioOps)
}
