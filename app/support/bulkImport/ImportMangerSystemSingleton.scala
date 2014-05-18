package support.bulkImport

import akka.actor._
import models.Transformer
import play.Logger

object ImportMangerSystemSingleton {
  private var mySystem: ImportMangerSystem = null
  private final val SYSTEMNAME: String = "sendR"

  def getInstance: ImportMangerSystem = {
    if (mySystem == null) {
      mySystem = new ImportMangerSystem(ActorSystem.create(SYSTEMNAME))
    }
    mySystem
  }

}

class ImportMangerSystem(system: ActorSystem) {

  def startImportManager(workers: Int, transformer: Transformer) {
    val supervisor = findTransformer(transformer.id)
    var creationNeeded : Boolean = true
    if (supervisor != null) {
      if (supervisor.isTerminated) {
        Logger.debug("Supervisor found terminated")
      }
      else {
        supervisor.tell("Lets restart", supervisor)
        supervisor.tell(new SupervisorCommand(SupervisorCommand.Status.START), supervisor)
        creationNeeded = false
      }
    }
    if (creationNeeded) {
      val importManager = system.actorOf(Props(new ImportSupervisorActor(workers, transformer)), transformer.name + "-" + transformer.id)
      addTransformer(transformer.id, importManager)
      Logger.info("Start import of " + transformer.importPath)
    }
  }

  def stopImportManager(id: Long) {
    val supervisor: ActorRef = findTransformer(id)
    if (supervisor != null) {
      supervisor.tell(PoisonPill.getInstance, supervisor)
      removeTransformer(id)
    }
  }

  def pauseImportManager(id: Long) {
    val supervisor: ActorRef = findTransformer(id)
    if (supervisor != null) {
      supervisor.tell(new SupervisorCommand(SupervisorCommand.Status.PAUSE), supervisor)
    }
  }

  def reportOnAllSuperVisors() {
    import scala.collection.JavaConversions._
    for (actor <- map.values) {
      actor.tell(new SupervisorCommand(SupervisorCommand.Status.REPORT), actor)
    }
  }

  private val map: java.util.Map[Long, ActorRef] = new java.util.HashMap[Long, ActorRef]

  private def findTransformer(id: Long): ActorRef = {
    map.get(id)
  }

  private def addTransformer(id: Long, actor: ActorRef) {
    map.put(id, actor)
  }

  private def removeTransformer(id: Long) {
    if (map.containsKey(id))
      map.remove(id)
  }



}