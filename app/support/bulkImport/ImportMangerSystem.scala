package support.bulkImport

import akka.actor._
import models.Transformer
import play.Logger
import actors.TransformerSupervisorActor

object ImportMangerSystem {

  private final val SYSTEMNAME: String = "sendR"
  var system = ActorSystem.create(SYSTEMNAME)


  def startTransformerSupervisor(workers: Int, transformer: Transformer) {
    val supervisor = findTransformer(transformer.id)
    if (supervisor != null) {
        supervisor.tell("Lets restart", supervisor)
        supervisor.tell(new SupervisorCommand(SupervisorCommandType.START), supervisor)
    } else {
        addTransformer(transformer.id, system.actorOf(Props(new TransformerSupervisorActor(workers, transformer)), transformer.name + "-" + transformer.id))
        Logger.info("Start import of " + transformer.importPath)
    }
  }

  def stopTransformerSupervisor(id: Long) {
    val supervisor: ActorRef = findTransformer(id)
    if (supervisor != null) {
      supervisor.tell(PoisonPill.getInstance, supervisor)
      removeTransformer(id)
    }
  }

  def pauseTransformerSupervisor(id: Long) {
    val supervisor: ActorRef = findTransformer(id)
    if (supervisor != null) {
      supervisor.tell(new SupervisorCommand(SupervisorCommandType.PAUSE), supervisor)
    }
  }

  def reportOnAllSuperVisors() {
    import scala.collection.JavaConversions._
    for (actor <- map.values) {
      actor.tell(new SupervisorCommand(SupervisorCommandType.REPORT), actor)
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