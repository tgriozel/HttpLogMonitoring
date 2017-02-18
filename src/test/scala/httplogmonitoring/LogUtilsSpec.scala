package httplogmonitoring

import org.specs2.mutable.Specification

class LogUtilsSpec extends Specification {
  "The CommonLogUtils" should {
    "Parse an empty log entry without throwing an exception" in {
      LogUtils.logLineToEntryData("") mustEqual LogEntryData()
      LogUtils.logLineToEntryData("- - - - - - - - -") mustEqual LogEntryData()
    }

    "Parse an incorrect entry without throwing an exception" in {
      LogUtils.logLineToEntryData("qwerty azerty 012345") mustEqual LogEntryData()
    }

    "Partially parse a partially complete log entry" in {
      val data = LogUtils.logLineToEntryData("- - - [10/Oct/2002:23:44:03 -0600] \"GET /app.js HTTP/1.0\" - -")
      data.time.get.toString mustEqual "2002-10-10T23:44:03-06:00"
      data.method mustEqual Some("GET")
      data.resource mustEqual Some("/app.js")
    }

    "Parse a full and valid log entry" in {
      val ex1 = "127.0.0.1 user-identifier frank [10/Oct/2000:13:55:36 -0700] \"GET /apache_pb.gif HTTP/1.0\" 200 232"
      val ex1data = LogUtils.logLineToEntryData(ex1)
      ex1data.host mustEqual Some("127.0.0.1")
      ex1data.userId mustEqual Some("frank")
      ex1data.method mustEqual Some("GET")
      ex1data.resource mustEqual Some("/apache_pb.gif")
      ex1data.status mustEqual Some(200)
      ex1data.bytes mustEqual Some(232)
      ex1data.time.get.toString mustEqual "2000-10-10T13:55:36-07:00"

      val ex2 = "216.67.1.91 - leon [01/Jul/2002:12:11:52 +0000] \"GET /index.html HTTP/1.1\" 200 431"
      val ex2data = LogUtils.logLineToEntryData(ex2)
      ex2data.host mustEqual Some("216.67.1.91")
      ex2data.userId mustEqual Some("leon")
      ex2data.method mustEqual Some("GET")
      ex2data.resource mustEqual Some("/index.html")
      ex2data.status mustEqual Some(200)
      ex2data.bytes mustEqual Some(431)
      ex2data.time.get.toString mustEqual "2002-07-01T12:11:52Z"
    }
  }
}
