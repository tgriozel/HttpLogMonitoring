package httplogmonitoring

import java.util.concurrent.{ScheduledThreadPoolExecutor, TimeUnit}

object Main {
  private val config = com.typesafe.config.ConfigFactory.load()
  private val logFileName = config.getString("input.log-file")
  private val logPollingPeriodMilliseconds = config.getInt("input.log-polling-period-ms")
  private val summaryObservationPeriod = config.getInt("alerter.summary-observation-period")
  private val summaryRequestPeriod = config.getInt("alerter.summary-request-period")
  private val trafficObservationPeriod = config.getInt("alerter.traffic-observation-period")
  private val trafficRequestPeriod = config.getInt("alerter.traffic-request-period")
  private val trafficThreshold = config.getInt("alerter.traffic-threshold")
  private val fileWatcher = new LogFileWatcher(logFileName, logPollingPeriodMilliseconds)
  private val dataStore = new LogDataStore
  private val logAlerter = LogAlerter(System.out, fileWatcher, dataStore, summaryObservationPeriod,
    trafficObservationPeriod, trafficThreshold)

  private object summaryRequester extends Runnable {
    override def run(): Unit = logAlerter.handleRequest(SummaryLogAlerterRequest)
  }

  private object trafficRequester extends Runnable {
    override def run(): Unit = logAlerter.handleRequest(TrafficCheckLogAlerterRequest)
  }

  def main(args: Array[String]): Unit = {
    val executor = new ScheduledThreadPoolExecutor(2)
    // Run periodically a summary requester and a traffic check requester, until the process is externally terminated
    executor.scheduleAtFixedRate(summaryRequester, summaryRequestPeriod, summaryRequestPeriod, TimeUnit.SECONDS)
    executor.scheduleAtFixedRate(trafficRequester, trafficRequestPeriod, trafficRequestPeriod, TimeUnit.SECONDS)
  }
}
