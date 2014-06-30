package support.bulkImport

import org.joda.time.DateTime

/**
 * Created with IntelliJ IDEA.
 * User: matthijs
 * Date: 6/29/13
 * Time: 7:46 PM
 * To change this template use File | Settings | File Templates.
 */


object WorkerResultStatus extends Enumeration {
  type Status = Value
  val DONE, FAILED, TIMEOUT, READY, SUICIDE, NO_WORK = Value
}

class WorkerResult(state: WorkerResultStatus.Status, result : Option[String], payLoad: Option[Payload]) {
  private val end: DateTime = new DateTime

  def status: WorkerResultStatus.Status = state
  def getResult: String = result.getOrElse("")

  def getProcessingTime: Long = {
    payLoad  match {
      case Some(pl)  =>
        end.getMillis - pl.getCreationDate.getMillis
      case None =>
        0L
    }
  }

  def getPayLoad : Option[Payload] = payLoad

}