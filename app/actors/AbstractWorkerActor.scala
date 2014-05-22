package actors

import akka.actor.ActorRef
import akka.actor.UntypedActor
import play.Logger
import support.bulkImport.{WorkerResult, WorkerResultStatus, Payload}

abstract class AbstractWorkerActor(mySupervisor: ActorRef) extends UntypedActor {

  def onReceive(message: Any) {

    message match {
      case payload : Payload =>
        mySupervisor.tell(processPayload(payload, new WorkerResult(WorkerResultStatus.READY)), getSelf())
      case _ =>
        val result: WorkerResult = new WorkerResult(WorkerResultStatus.FAILED)
        result.setResult("I do not know what you want me to do with this.")
        sender.tell(result, getSelf())
    }
  }

  protected def processPayload(payload: Payload, result: WorkerResult) : WorkerResult

  override def preStart() {
    Logger.debug(self.toString + " - Starting worker")
    if (mySupervisor != null)
      mySupervisor.tell(new WorkerResult(WorkerResultStatus.READY), getSelf())
  }

  override def postStop() {
    Logger.debug(self.toString + " - Terminated worker ")
    mySupervisor.tell(new WorkerResult(WorkerResultStatus.SUICIDE), getSelf())
  }

}