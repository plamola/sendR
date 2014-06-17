package actors

import akka.actor.ActorRef
import play.Logger
import support.bulkImport.{WorkerResult, WorkerResultStatus, Payload}
import java.util.Random
import java.lang.Thread.sleep

/**
 * Created with IntelliJ IDEA.
 * User: matthijs
 * Date: 6/30/13
 * Time: 4:30 PM
 * To change this template use File | Settings | File Templates.
 */
class DummyWorkerActor(var inJobController : ActorRef) extends AbstractWorkerActor(inJobController) {


  protected def processPayload(payload: Payload) : WorkerResult = {
    try {
      val rand: Random = new Random
      sleep((rand.nextInt(200) + 1) * 100)
      new WorkerResult(WorkerResultStatus.DONE,None,None)
    }
    catch {
      case e: Exception =>
        Logger.error("Dummy processor failed with error " + e.getMessage)
        new WorkerResult(WorkerResultStatus.FAILED,None,None)
      }
  }
}