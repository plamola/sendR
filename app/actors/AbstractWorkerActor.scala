package actors

import akka.actor.ActorRef
import akka.actor.UntypedActor
import play.Logger
import support.bulkImport.{WorkerResultStatus, Payload, WorkerResult}

abstract class AbstractWorkerActor(mySupervisor: ActorRef) extends UntypedActor {

  def onReceive(message: Any) {
    val result: WorkerResult = new WorkerResult(WorkerResultStatus.READY)
    message match {
      case payload : Payload =>
        processPayload(payload, result)
        mySupervisor.tell(result, getSelf())
      case _ =>
        Logger.debug("I do not know what you want me to do with this.")
        result.setResult("I do not know what you want me to do with this.")
        result.status=WorkerResultStatus.FAILED
        sender.tell(result, getSelf())
    }
  }

  protected def processPayload(payload: Payload, result: WorkerResult)

  override def preStart() {
    Logger.debug(self.toString + " - Starting worker")
    if (mySupervisor != null)
      mySupervisor.tell(new WorkerResult(WorkerResultStatus.READY), getSelf())
  }

  override def postStop() {
    Logger.debug(self.toString + " - Terminated worker ")
    mySupervisor.tell(new WorkerResult(WorkerResultStatus.SUICIDE), getSelf())
  }

  //private final val mySupervisor: ActorRef = inJobController
}