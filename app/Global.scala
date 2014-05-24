import actors.StatusReportActor
import akka.actor.{Props, ActorSystem}
import models.Transformer
import models.User
import play.api.libs.concurrent.Akka
import play.api.Logger
import play.Application
import play.GlobalSettings
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import play.api.Play.current
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global

class Global extends GlobalSettings {

  override def onStart(app: Application) {
    InitialData.insert()
    startReporter()
    Logger.info("Application has started")
  }

  override def onStop(app: Application) {
    Logger.info("Application shutdown...")
  }

  def startReporter() {
    implicit var system = ActorSystem.create("reporting")
    val reporter = system.actorOf(Props[StatusReportActor],"statusreport")
    Akka.system.scheduler.schedule(
      Duration.create(0, TimeUnit.MILLISECONDS),
      Duration.create(10, TimeUnit.SECONDS),
      reporter, "status report please")
  }

}

