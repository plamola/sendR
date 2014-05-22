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
  type  Status = Value
  val DONE,FAILED,TIMEOUT,READY,SUICIDE = Value

}

class WorkerResult(state: WorkerResultStatus.Status) {

  private var result: String = null
  private var failedInput: String = null
  private var lineNumber: Long = 0L
  private val start: DateTime = new DateTime
  private var end: DateTime = this.start
  var status: WorkerResultStatus.Status = state



  def getFailedInput: String = {
    failedInput
  }

  def setFailedInput(failedInput: String) {
    this.failedInput = failedInput
  }

  def getLineNumber: Long = {
    lineNumber
  }

  def setLineNumber(lineNumber: Long) {
    this.lineNumber = lineNumber
  }


//  def getStatus: WorkerResultStatus.Status = {
//    this.status
//  }
//
//  def setStatus(status: WorkerResultStatus.Status) {
//    this.status = status
//    this.end = new DateTime
//  }

  def getResult: String = {
    result
  }

  def setResult(result: String) {
    this.result = result
  }

  def getProcessingTime: Long = {
    end.getMillis - start.getMillis
  }

}