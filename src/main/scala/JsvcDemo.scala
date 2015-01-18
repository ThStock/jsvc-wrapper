import java.util.Timer
import java.util.TimerTask
import java.io.File

import com.typesafe.scalalogging.LazyLogging
import org.apache.commons.daemon.Daemon
import org.slf4j.MarkerFactory

object JsvcDemo extends App {
  new JsvcDemoDaemon().start
}

class JsvcDemo extends RobustTimerTask {

  val some = MarkerFactory.getMarker("some")

  def work() {
    logger.info(some, " hello ...")
    val markerNames = Seq("re", "ex", "er", "th")
    val existingMarkerFiles = markerNames.map(new File(_)).filter(_.exists)
    if (existingMarkerFiles.nonEmpty) {
      val markerFile = existingMarkerFiles.head
      markerFile.delete
      markerFile.getName match {
        case "re" => throw new IllegalStateException("re")
        case "ex" => throw new Exception("ex")
        case "er" => throw new Error("er")
        case _ => throw new Throwable("th")
      }
    }
  }
}

class JsvcDemoDaemon extends Daemon with LazyLogging {

  val timer:Timer = new Timer()

  def destroy() { logger.debug("destroy") }

  def init(context: org.apache.commons.daemon.DaemonContext) {
    logger.debug("init")
    logger.debug(context.toString)
  }

  def start() {
    logger.debug("start")
    timer.schedule(new RobustTimerTask() {
      def work() {
        logger.info(" running ...")
      }
    }, 0, 1000)
    timer.schedule(new JsvcDemo(), 0, 500)
  }

  def stop() {
    logger.debug("stopping")
    timer.cancel()
    logger.debug("stopped")
  }
}

trait RobustTimerTask extends TimerTask with LazyLogging {

  def run() {
    try {
      work()
    } catch {
      case e:RuntimeException => logger.error("", e)
      case e:Exception => logger.error("", e)
      case e:Error => logger.error("", e)
      case e:Throwable => logger.error("", e)
    }
  }

  def work()
}
