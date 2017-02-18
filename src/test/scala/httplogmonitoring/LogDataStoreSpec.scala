package httplogmonitoring

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import org.specs2.mutable.Specification

class LogDataStoreSpec extends Specification {
  // We insert and remove data here, so we need to do things sequentially
  sequential

  "The LogDataStore" should {
    val request = "\"GET /app.js HTTP/1.0\""
    val formatter = DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z")
    val now = ZonedDateTime.now
    val dataStore = new LogDataStore

    // Tests are timing-sensitive because now() is used internally, so let's use large time spans
    dataStore.ingestNewLogLines(List(
      s"- - - [${now.minusSeconds(1).format(formatter)}] $request - -",
      s"- - - [${now.minusSeconds(1).format(formatter)}] $request - -",
      s"- - - [${now.minusSeconds(60).format(formatter)}] $request - -",
      s"- - - [${now.minusSeconds(60).format(formatter)}] $request - -",
      s"- - - [${now.minusSeconds(60).format(formatter)}] $request - -",
      s"- - - [${now.minusSeconds(120).format(formatter)}] $request - -",
      s"- - - [${now.minusSeconds(120).format(formatter)}] $request - -",
      s"- - - [${now.minusSeconds(120).format(formatter)}] $request - -"
    ))

    "Return the correct data" in {
      dataStore.entriesFromSecondsAgo(0).size mustEqual 0
      dataStore.entriesFromSecondsAgo(30).size mustEqual 2
      dataStore.entriesFromSecondsAgo(90).size mustEqual 5
      dataStore.entriesFromSecondsAgo(180).size mustEqual 8
    }

    "Correctly clear data when asked for" in {
      dataStore.clearOlderThanSecondsAgo(90)
      dataStore.entriesFromSecondsAgo(90).size mustEqual 5
      dataStore.clearOlderThanSecondsAgo(120)
      dataStore.entriesFromSecondsAgo(90).size mustEqual 5
    }
  }
}
