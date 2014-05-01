package support.bulkImport

import org.joda.time.DateTime

/**
 * Created with IntelliJ IDEA.
 * User: matthijs
 * Date: 7/11/13
 * Time: 8:59 PM
 * To change this template use File | Settings | File Templates.
 */
class SupervisorState {
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

  def setTransformerName(transformerName: String) {
    this.transformerName = transformerName
  }

  def getTransformerId: Long = {
    transformerId
  }

  def setTransformerId(transformerId: Long) {
    this.transformerId = transformerId
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

  def setWorkers(count: Int) {
    this.workers = count
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

  def getStatus: ImportSupervisorActor.Status = {
    status
  }

  def setStatus(status: ImportSupervisorActor.Status) {
    this.status = status
  }

  private var succesCount: Int = 0
  private var failureCount: Int = 0
  private var timeOutcount: Int = 0
  private var startTime: DateTime = new DateTime
  private var stopTime: DateTime = new DateTime
  private var workers: Int = 0
  private var activeWorkers: Int = 0
  private var payloadCount: Int = 0
  private var status: ImportSupervisorActor.Status = ImportSupervisorActor.Status.STOPPED
  private var transformerId: Long = 0L
  private var transformerName: String = null
  private var currentFile: String = null
  private var nrOfLines: Long = 0L
}