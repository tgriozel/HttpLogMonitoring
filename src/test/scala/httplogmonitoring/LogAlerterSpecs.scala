package httplogmonitoring

import java.io.{OutputStream, PrintStream}

import org.mockito.Mockito
import org.mockito.Matchers._
import org.specs2.mutable.Specification

class LogAlerterSpecs extends Specification {
  "The LogAlerter" should {
    // More in-depth tests (checking the output) could be done with an adapted PrintStream object
    val nullPrintStream = new PrintStream(new OutputStream {override def write(b: Int): Unit = {}})
    val alerter = LogAlerter(nullPrintStream, new LogFileWatcher("dummy"), new LogDataStore, 0, 0, 0)

    "Start and stay in normal state if there is no reason to freak out" in {
      alerter.currentState mustEqual NormalTrafficState
      alerter.handleRequest(TrafficCheckLogAlerterRequest) mustEqual NormalTrafficState
      alerter.handleRequest(SummaryLogAlerterRequest) mustEqual NormalTrafficState
    }

    "Switch state when needed" in {
      alerter.currentState mustEqual NormalTrafficState
      alerter.handleRequest(TrafficCheckLogAlerterRequest) mustEqual NormalTrafficState
      val spiedAlerter = Mockito.spy(alerter)
      Mockito.when(spiedAlerter.isThresholdExceeded(anyInt)).thenReturn(true)
      spiedAlerter.handleRequest(TrafficCheckLogAlerterRequest) mustEqual HighTrafficState
      Mockito.when(spiedAlerter.isThresholdExceeded(anyInt)).thenReturn(false)
      spiedAlerter.handleRequest(TrafficCheckLogAlerterRequest) mustEqual NormalTrafficState
    }
  }
}
