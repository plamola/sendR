package support.bulkImport.workers

import akka.actor.ActorRef
import akka.actor.UntypedActor
import play.Logger
import support.bulkImport.Payload
import support.bulkImport.WorkerResult

abstract class AbstractWorkerActor(mySupervisor: ActorRef) extends UntypedActor {

  def onReceive(message: Any) {
    val result: WorkerResult = new WorkerResult
    message match {
      case payload : Payload =>
        processPayload(payload, result)
        mySupervisor.tell(result, getSelf())
      case _ =>
        Logger.debug("I do not know what you want me to do with this.")
        result.setResult("I do not know what you want me to do with this.")
        result.setStatus(WorkerResult.Status.FAILED)
        sender.tell(result, getSelf())
    }
  }

  protected def processPayload(payload: Payload, result: WorkerResult)

  override def preStart() {
    Logger.debug(self.toString + " - Starting worker")
    if (mySupervisor != null)
      mySupervisor.tell(new WorkerResult(WorkerResult.Status.READY), getSelf())
  }

  override def postStop() {
    Logger.debug(self.toString + " - Terminated worker ")
    mySupervisor.tell(new WorkerResult(WorkerResult.Status.SUICIDE), getSelf())
  }

  //private final val mySupervisor: ActorRef = inJobController
}