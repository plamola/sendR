package support.bulkImport

import org.joda.time.DateTime
import models.Transformer

/**
 * Created with IntelliJ IDEA.
 * User: matthijs
 * Date: 7/11/13
 * Time: 8:59 PM
 * To change this template use File | Settings | File Templates.
 */


object SupervisorStateType  extends Enumeration {
  type Status = Value
  val STARTING, RUNNING,PAUSING,PAUSED,RESUMING,STOPPING,STOPPED = Value
}

class SupervisorState(workers: Int, transformer: Transformer) {
  def getNrOfLines: Long = {
    nrOfLines
  }

  def getCurrentFile: String = {
    currentFile
  }

  def setCurrentFileSpecs(currentFile: String, nrOfLines: Long) {
    this.currentFile = currentFile
    this.nrOfLines = nrOfLines
  }

  def getTransformerName: String = {
    transformerName
  }

  def getTransformerId: Long = {
    transformerId
  }

  def getSuccesCount: Int = {
    succesCount
  }

  def incrementSuccesCount() {
    this.succesCount += 1
    resetTimeOutCount()
  }

  def getFailureCount: Int = {
    failureCount
  }

  def incrementFailureCount() {
    this.failureCount += 1
    resetTimeOutCount()
  }

  def getTimeOutcount: Int = {
    timeOutcount
  }

  def resetTimeOutCount() {
    this.timeOutcount = 0
  }

  def incrementTimeOutCount() {
    this.timeOutcount += 1
  }

  def getStartTime: DateTime = {
    startTime
  }

  def setStartTime(startTime: DateTime) {
    this.startTime = startTime
  }

  def getStopTime: DateTime = {
    stopTime
  }

  def setStopTime(stopTime: DateTime) {
    this.stopTime = stopTime
  }

  def getWorkers: Int = {
    workers
  }

  def incrementActiveWorkers() {
    activeWorkers += 1
  }

  def decrementActiveWorkers() {
    activeWorkers -= 1
  }

  def getActiveWorkers: Int = {
    activeWorkers
  }

  def getPayloadCount: Int = {
    payloadCount
  }

  def incrementPayloadCount() {
    this.payloadCount += 1
  }

  def getStatus: SupervisorStateType.Status = {
    status
  }

  def setStatus(newStatus: SupervisorStateType.Status) {
   status = newStatus
  }

  private var succesCount: Int = 0
  private var failureCount: Int = 0
  private var timeOutcount: Int = 0
  private var startTime: DateTime = new DateTime
  private var stopTime: DateTime = new DateTime
  private var activeWorkers: Int = 0
  private var payloadCount: Int = 0
  private var status: SupervisorStateType.Status = SupervisorStateType.STOPPED
  private val transformerId: Long = transformer.id
  private val transformerName: String = transformer.name
  private var currentFile: String = null
  private var nrOfLines: Long = 0L
}