package httplogmonitoring

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern

import scala.util.Try

case class LogEntryData(host: Option[String] = None, userId: Option[String] = None,
                        time: Option[ZonedDateTime] = None, method: Option[String] = None,
                        resource: Option[String] = None, status: Option[Int] = None, bytes: Option[Int] = None)

object LogUtils {
  // The patterns here correspond to the "Common Log Format", also known as the "NCSA Common log format".
  // Some fields are optional and can be omitted, in this case, they just need to be marked with "-".
  private val maxFieldsCount = 9
  private val entryPattern = Pattern.compile("^([\\d.]+|-) (\\S+) (\\S+) \\[([\\w:/]+\\s[+\\-]\\d{4})\\] " +
                                             "\"(\\S+) (\\S+) (\\S+)\" (\\d{3}|-) (\\d+|-)")

  private val commonLogDateFormat = "dd/MMM/yyyy:HH:mm:ss Z"
  private val dateFormatter = DateTimeFormatter.ofPattern(commonLogDateFormat)

  private def parsedFieldToOption(field: String): Option[String] = field match {
    case s if s.isEmpty || s == "-" => None
    case s => Option(s)
  }

  def logLineToEntryData(line: String): LogEntryData = {
    val matcher = entryPattern.matcher(line)
    if (matcher.matches && matcher.groupCount <= maxFieldsCount) {
      val hostOpt = parsedFieldToOption(matcher.group(1))
      val userIdOpt = parsedFieldToOption(matcher.group(3))
      val timeStrOpt = parsedFieldToOption(matcher.group(4))
      val methodOpt = parsedFieldToOption(matcher.group(5))
      val resourceOpt = parsedFieldToOption(matcher.group(6))
      val statusStrOpt = parsedFieldToOption(matcher.group(8))
      val bytesStrOpt = parsedFieldToOption(matcher.group(9))

      val timeOpt = timeStrOpt match {
        case None => None
        case Some(str) => Try(ZonedDateTime.parse(str, dateFormatter)).toOption
      }
      val statusOpt = statusStrOpt match {
        case None => None
        case Some(str) => Try(Integer.valueOf(str).intValue).toOption
      }
      val bytesOpt = bytesStrOpt match {
        case None => None
        case Some(str) => Try(Integer.valueOf(str).intValue).toOption
      }

      LogEntryData(hostOpt, userIdOpt, timeOpt, methodOpt, resourceOpt, statusOpt, bytesOpt)
    }
    else {
      LogEntryData()
    }
  }
}
