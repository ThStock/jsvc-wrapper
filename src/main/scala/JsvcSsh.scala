import org.apache.sshd.SshServer
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider
import org.apache.sshd.server.PasswordAuthenticator
import org.apache.sshd.server.session.ServerSession
import jline.console.ConsoleReader
import jline.console.completer.StringsCompleter
import org.apache.sshd.common.Factory
import org.apache.sshd.server.Command
import org.apache.sshd.server.Environment
import org.apache.sshd.server.ExitCallback
import com.typesafe.scalalogging.LazyLogging
import java.io._
import scala.collection.JavaConverters._

object JsvcSsh {

  trait ShellAdapter extends Command with Runnable with LazyLogging {

    var in:InputStream = null
    var out:OutputStream = null
    var err:OutputStream = null
    var callback:ExitCallback = null
    var environment:Environment = null
    var reader:ConsoleReader = null
    var writer:PrintWriter = null
    var thread:Thread = null

    def setInputStream(_in:InputStream) { in = _in }
    def setOutputStream(_out:OutputStream) { out = _out }

    def setErrorStream(_err:OutputStream) { err = _err }
    def setExitCallback(_callback:ExitCallback) { callback = _callback}

    def start(env:Environment) {
      environment = env
      thread = new Thread(this, "ShellAdapter")
      thread.start()
    }

    def handle(f:(String) => Unit) = {
      Iterator.continually(reader.readLine())
        .takeWhile(_ != null)
        .filter(_.nonEmpty)
        .foreach(line => f(line.trim))
    }

    def destroy() {
      if (reader != null) {
        if (in != null) {
         in.close()
        }
        reader.shutdown()
        thread.interrupt()
      }
    }

    def finish() {
      writeln("Shutting down service")
      callback.onExit(0)
    }

    def writeln(lines:String) {
      writer.println(lines)
      writer.flush()
    }

    def prompt()

    def run() {
      try {
        prompt()
      } catch {
        case e:InterruptedIOException => logger.debug("", e)
        case e:IOException => {
          logger.debug("Error executing...", e)
          logger.warn("IOException ... see debug for details")
        }
        case e:Exception => logger.warn("Error executing...", e)
      } finally {
        finish()
      }
    }

    def exitOn(line:String, params:Seq[String]) {
      if (params.contains(line)) {
        throw new InterruptedIOException()
      }
    }

    def initPrompt(prompt:String, completer:Seq[String]) {
      reader = new ConsoleReader(in, new FilterOutputStream(out) {
        override def write(c:Int) {
          super.write(c)
          if (c == ConsoleReader.CR.toCharArray()(0)) {
            super.write(ConsoleReader.RESET_LINE)
          }
        }
      })
      reader.setPrompt(prompt)
      reader.addCompleter(new StringsCompleter(completer.asJava))
      writer = new PrintWriter(reader.getOutput())
    }
  }
}

class JsvcSsh(port:Int, adapter:() => JsvcSsh.ShellAdapter) extends LazyLogging {
  val sshd = SshServer.setUpDefaultServer()
  var adapters:Seq[JsvcSsh.ShellAdapter] = Nil
  sshd.setPort(port)
  sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider("/tmp/hostkey.ser"))
  sshd.setPasswordAuthenticator(new PasswordAuthenticator() {
    def authenticate(username:String, password:String, session:ServerSession):Boolean = {
      return true
    }
  })

  sshd.setShellFactory(new Factory[Command] {
    def create():Command = {
      val result = adapter()
      adapters = adapters :+ result
      return result
    }
  })


  def start() {
    sshd.start()
    logger.info("Ready for connections on port " + port)
  }

  def stop() {
    adapters.foreach(_.destroy())
    adapters.foreach(_.finish())
    sshd.stop()
  }

}
