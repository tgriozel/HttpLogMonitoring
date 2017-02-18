package httplogmonitoring

import java.time._
import java.time.temporal.ChronoUnit

import scala.annotation.tailrec
import scala.collection.immutable.SortedMap

class LogDataStore {
  implicit val zonedDateTimeOrder: Ordering[ZonedDateTime] = Ordering.fromLessThan(_ isBefore _)
  private val defaultZoneId = ZoneId.systemDefault
  // This is a variable pointing to an immutable map, synchronized accesses ensure overall integrity
  private var map = SortedMap.empty[ZonedDateTime, List[LogEntryData]]

  // Log lines are associated to their embedded instant data, adjusted to the system zone
  def ingestNewLogLines(lines: List[String]): Unit = this.synchronized {
    val newMap = lines.map(LogUtils.logLineToEntryData).filter(_.time.isDefined)
      .groupBy(_.time.get.withZoneSameInstant(defaultZoneId))

    map = newMap.foldLeft(SortedMap.empty[ZonedDateTime, List[LogEntryData]]) { case (accMap, (zonedDateTime, list)) =>
      accMap.updated(zonedDateTime, map.getOrElse(zonedDateTime, List.empty) ::: list)
    }
  }

  // Find closest existing key (going forward in time), assuming candidate key is in range (ensured by calling methods)
  @tailrec
  private def findClosestKeyInMap(candidateKey: ZonedDateTime): ZonedDateTime = {
    if (map.contains(candidateKey))
      candidateKey
    else
      findClosestKeyInMap(candidateKey.plusSeconds(1))
  }

  // Return all entries starting from current time minus a given amount of seconds
  def entriesFromSecondsAgo(seconds: Int): List[LogEntryData] = this.synchronized {
    val zonedDateTime = ZonedDateTime.now(defaultZoneId).truncatedTo(ChronoUnit.SECONDS).minusSeconds(seconds)
    if (map.isEmpty || map.keys.last.compareTo(zonedDateTime) < 0) {
      List.empty
    }
    else {
      val key = if (map.keys.head.compareTo(zonedDateTime) > 0) map.keys.head else findClosestKeyInMap(zonedDateTime)
      map.from(key).values.toList.flatten
    }
  }

  // Discard all entries older than a given amount of seconds, to reduce memory usage
  def clearOlderThanSecondsAgo(seconds: Int): Unit = this.synchronized {
    val zonedDateTime = ZonedDateTime.now(defaultZoneId).truncatedTo(ChronoUnit.SECONDS).minusSeconds(seconds)
    if (map.nonEmpty && map.keys.last.compareTo(zonedDateTime) >= 0) {
      map = map.filter { case (mapKey, _) =>
        zonedDateTime.isBefore(mapKey)
      }
    }
  }
}
