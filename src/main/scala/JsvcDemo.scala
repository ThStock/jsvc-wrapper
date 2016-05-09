import java.util.Timer
import java.util.TimerTask

import com.typesafe.scalalogging.LazyLogging
import org.apache.commons.daemon.Daemon
import org.slf4j.MarkerFactory

object JsvcDemo extends App {
  new JsvcDemoDaemon().start
}

class JsvcDemoDaemon extends Daemon with LazyLogging {

  val timer:Timer = new Timer()
  val loggerTask = new JsvcDemo()
  val shell = new JsvcSsh(5222, () => new ExceptionShell(loggerTask))

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
    }, 0, 3000)
    timer.schedule(loggerTask, 0, 500)
    shell.start()
  }

  def stop() {
    shell.stop()
    timer.cancel()
    logger.debug("stopped")
  }
}

class JsvcDemo extends RobustTimerTask {

  val some = MarkerFactory.getMarker("some")

  var status:Seq[String] = Nil

  def work() {
    logger.info(some, " hello ...")
    if (status.nonEmpty) {
      val currentState = status.head
      status = status.takeRight(status.size -1)
      currentState match {
        case "re" => throw new IllegalStateException("re")
        case "ex" => throw new Exception("ex")
        case "er" => throw new Error("er")
        case _ => throw new Throwable("th")
      }
    }
  }
}

class ExceptionShell(task:JsvcDemo) extends JsvcSsh.ShellAdapter {

  val SHELL_CMD_QUIT = "quit"
  val SHELL_CMD_EXIT = "exit"
  val SHELL_CMD_HELP = "help"

  def handleUserInput(line:String) {
    exitOn(line, Seq(SHELL_CMD_QUIT, SHELL_CMD_EXIT))

    if (line.equalsIgnoreCase(SHELL_CMD_HELP)) {
      writeln("Possible values are: " + Seq("re", "ex", "er", "th"))
    } else {
      writeln("=> \"" + line + "\"")
      task.status = line.split(" ").toSeq
    }
  }

  def prompt() {
    val params = Seq(SHELL_CMD_QUIT, SHELL_CMD_EXIT, SHELL_CMD_HELP)
    initPrompt(prompt = "eX> ", completer = params)

    writeln("""
      |*******************************
      |* Welcome to Exception Shell. *
      |*******************************""".stripMargin.trim)

    handle((line) => handleUserInput(line))
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
