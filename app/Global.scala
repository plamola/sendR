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
    ExistingData.upgrade()
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

/**
 * Initial set of data to be loaded
 */
object InitialData {
  def insert() {
    val defaultEmail : String = "sendr@localhost"
    User.findByEmail(defaultEmail) match {
      case Some(user) => // Nothing to create
      case None => User.create(defaultEmail,"klJJS13j#k")
    }

    // TODO Fix Initial Data
//    if (Ebean.find(classOf[Transformer]).findRowCount eq 0) {
//      val all: Map[String, java.util.List[AnyRef]] = Yaml.load("transformers.yml").asInstanceOf[Map[String, List[AnyRef]]]
//      import scala.collection.JavaConversions._
//      for (key <- all.keySet) {
//        Ebean.save(all.get(key))
//      }
    }
  }

object ExistingData {
  def upgrade() {
    // TODO Fix Existing Data
//    for (transformer <- Transformer.all) {
//      if (transformer.version < 1) {
//        val message: String = transformer.webserviceTemplate
//        transformer.webserviceTemplate = message.replace("{eisTimeStamp}", "{timestamp}")
//        transformer.version = 1
//        Transformer.update(transformer)
//      }
//    }
  }

}