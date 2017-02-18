package httplogmonitoring

import java.io.File
import org.apache.commons.io.input.{Tailer, TailerListenerAdapter}

class LogFileWatcher(filePath: String, pollingPeriodMilliseconds: Int = 1000) extends TailerListenerAdapter {
  // Every pollingPeriodMilliseconds, poll for newly added lines, and store them in an internal buffer.
  // The buffer is cleared every time consumeLines() is called, so we don't get a gigantic memory footprint.
  // The Apache commons-io Tailer is used, it also correctly handles log rotation.
  private val buffer = collection.mutable.ArrayBuffer.empty[String]
  private val tailer = Tailer.create(new File(filePath), this, pollingPeriodMilliseconds)

  override def handle(line: String): Unit = this.synchronized {
    buffer += line
  }

  def consumeLines(): List[String] = this.synchronized {
    val result = buffer.toList
    buffer.clear()
    result
  }
}
