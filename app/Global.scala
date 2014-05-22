import akka.actor.{Props, ActorSystem}
import models.Transformer
import models.User
import play.api.libs.concurrent.Akka
import play.api.Logger
import play.Application
import play.GlobalSettings
import play.libs.Akka
import play.libs.Akka
import scala.concurrent.duration.Duration
import support.bulkImport.ImportMangerSystemSingleton
import support.bulkImport.ImportMangerSystem
import java.util.concurrent.TimeUnit



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
//    Akka.system.scheduler.schedule(
//      Duration.create(0, TimeUnit.MILLISECONDS),
//      Duration.create(10, TimeUnit.SECONDS), new Runnable {
//        def run {
//          val mgr: ImportMangerSystem = ImportMangerSystemSingleton.getInstance
//          mgr.reportOnAllSuperVisors
//        }
//      }, Akka.system.dispatcher)


  }


}



/**
 * Initial set of data to be loaded
 */
object InitialData {
  def insert() {
//    if (Ebean.find(classOf[User]).findRowCount eq 0) {
//      val all: Map[String, java.util.List[AnyRef]] = Yaml.load("initial-data.yml").asInstanceOf[Map[String, List[AnyRef]]]
//      Ebean.save(all.get("users"))
//    }
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