package actors

import akka.actor.ActorRef
import akka.actor.UntypedActor
import play.Logger
import support.bulkImport.{WorkerResult, WorkerResultStatus, Payload}

abstract class AbstractWorkerActor(mySupervisor: ActorRef) extends UntypedActor {

  def onReceive(message: Any) {

    message match {
      case payload : Payload =>
        mySupervisor ! processPayload(payload)
      case _ =>
        sender ! new WorkerResult(WorkerResultStatus.FAILED,Some("I do not know what you want me to do with this."), None)
    }
  }

  protected def processPayload(payload: Payload) : WorkerResult

  override def preStart() {
    Logger.debug(self.toString + " - Starting worker")
    if (mySupervisor != null)
      mySupervisor ! new WorkerResult(WorkerResultStatus.READY, None, None)
  }

  override def postStop() {
    Logger.debug(self.toString + " - Terminated worker ")
    mySupervisor ! new WorkerResult(WorkerResultStatus.SUICIDE,None, None)
  }

}