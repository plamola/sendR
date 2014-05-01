package support.bulkImport

import akka.actor._
import models.Transformer
import play.Logger

object ImportMangerSystem {
  private var mySystem: ImportMangerSystem2 = null
  private final val SYSTEMNAME: String = "SendRContol"

  def getInstance: ImportMangerSystem2 = {
    if (mySystem == null) {
      mySystem = new ImportMangerSystem2(ActorSystem.create(SYSTEMNAME))
    }
    mySystem
  }

}

class ImportMangerSystem2(system: ActorSystem) {

  private val map: java.util.Map[String, ActorRef] = new java.util.HashMap[String, ActorRef]

  private def findSupervisor(id: Long): ActorRef = {
    map.get(id)
  }

  private def addSupervisor(id: Long, actor: ActorRef) {
    map.put(id.toString, actor)
  }

  def reportOnAllSuperVisors() {
    import scala.collection.JavaConversions._
    for (actor <- map.values) {
      actor.tell(new SupervisorCommand(SupervisorCommand.Status.REPORT), actor)
    }
  }

  def startImportManager(workers: Int, tr: Transformer) {
//    transformer = tr
    val transformer: Transformer = tr
    val wrks: Int = workers
    val supervisor: ActorRef = findSupervisor(tr.id)
    if (supervisor != null) {
      if (supervisor.isTerminated) {
        Logger.debug("Supervisor found terminated")
      }
      else {
        supervisor.tell("Lets restart", supervisor)
        supervisor.tell(new SupervisorCommand(SupervisorCommand.Status.START), supervisor)
      }
    }
    val importManager: ActorRef = system.actorOf(Props.create(new UntypedActorFactory {
      def create(): UntypedActor = {
        new ImportSupervisorActor(wrks, transformer)
      }
    }), "SupervisorFor_" + transformer.name)
    addSupervisor(tr.id, importManager)
    Logger.info("Start import of " + transformer.importPath)
  }


  def stopImportManager(id: Long) {
    val supervisor: ActorRef = findSupervisor(id)
    if (supervisor != null) {
      supervisor.tell(PoisonPill.getInstance, supervisor)
    }
  }

  def pauseImportManager(id: Long) {
    val supervisor: ActorRef = findSupervisor(id)
    if (supervisor != null) {
      supervisor.tell(new SupervisorCommand(SupervisorCommand.Status.PAUSE), supervisor)
    }
  }
}