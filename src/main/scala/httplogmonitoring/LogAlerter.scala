package httplogmonitoring

import java.io.PrintStream

sealed trait LogAlerterRequest
object SummaryLogAlerterRequest extends LogAlerterRequest
object TrafficCheckLogAlerterRequest extends LogAlerterRequest

sealed trait LogAlerterState
object NormalTrafficState extends LogAlerterState
object HighTrafficState extends LogAlerterState

// This simulates some kind of finite state machine, with two possible states, receiving two possible requests.
// trafficCheckPeriod and summaryPeriod are the periods on which events are observed, they are not necessarily equal to
// the period of the associated requests (traffic check / summary).
case class LogAlerter(printer: PrintStream, fileWatcher: LogFileWatcher, dataStore: LogDataStore, summaryPeriod: Int,
                      trafficCheckPeriod: Int, trafficThreshold: Int) {

  private var state: LogAlerterState = NormalTrafficState

  def currentState: LogAlerterState = state

  private def fullResourceToSection(resource: String): String = {
    val startOffset = if (resource.startsWith("/")) 1 else 0
    resource.substring(startOffset, resource.indexOf('/'))
  }

  private def printSummary(): Unit = {
    dataStore.ingestNewLogLines(fileWatcher.consumeLines())
    val summaryPeriodEntries = dataStore.entriesFromSecondsAgo(summaryPeriod)

    val totalRequests = summaryPeriodEntries.size
    val totalBytesTransferred = summaryPeriodEntries.map(_.bytes.getOrElse(0)).sum
    val allStatuses = summaryPeriodEntries.flatMap(_.status)
    def totalNxxStatuses(N: Int) = allStatuses.count(status => status >= N * 100 && status < (N + 1) * 100)
    val sectionCount = summaryPeriodEntries.flatMap(_.resource).groupBy(fullResourceToSection).mapValues(_.size)
    val topSectionEntry = sectionCount.toIterator.foldLeft("" -> 0) { case (topEntry, (section, count)) =>
      if (topEntry._2 > count)
        topEntry
      else
        section -> count
    }

    printer.println(s"Summary for the last $summaryPeriod seconds:")
    printer.println(s"\tTotal requests: $totalRequests")
    printer.println(s"\tTotal bytes transferred: $totalBytesTransferred")
    printer.println(s"\tMost hit section: ${topSectionEntry._1} with ${topSectionEntry._2} hits")
    for (n <- 2 until 5)
      printer.println(s"\tTotal ${n}xx statuses: ${totalNxxStatuses(n)}")
  }

  private def requestsOverPeriod(): Int = {
    dataStore.ingestNewLogLines(fileWatcher.consumeLines())
    dataStore.entriesFromSecondsAgo(trafficCheckPeriod).size
  }

  def isThresholdExceeded(trafficValue: Int): Boolean = {
    trafficValue > trafficThreshold
  }

  def handleRequest(request: LogAlerterRequest): LogAlerterState = this.synchronized {
    request match {
      case SummaryLogAlerterRequest =>
        printSummary()
      case TrafficCheckLogAlerterRequest =>
        state match {
          case NormalTrafficState =>
            val count = requestsOverPeriod()
            if (isThresholdExceeded(count)) {
              printer.println(s"Traffic threshold exceeded: $count requests over the last $trafficCheckPeriod seconds")
              state = HighTrafficState
            }
          case HighTrafficState =>
            val count = requestsOverPeriod()
            if (!isThresholdExceeded(count)) {
              printer.println(s"Traffic back to normal: $count requests over the last $trafficCheckPeriod seconds")
              state = NormalTrafficState
            }
        }
    }
    state
  }
}
