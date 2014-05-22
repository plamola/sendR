package support.bulkImport

import akka.actor._
import akka.routing.RoundRobinRouter
import models.Transformer
import org.joda.time._
import play.Logger
import support.FileImporter
import support.Informer
import support.bulkImport.workers.WebserviceWorkerActor
import java.io._

object ImportSupervisorActor {

}

class ImportSupervisorActor(workers: Int, transformer: Transformer) extends UntypedActor {

  private final val MAX_TIMEOUTS: Int = 100
  private final val fileImporter: FileImporter = new FileImporter(transformer)
  private val supervisorState: SupervisorState = new SupervisorState(workers, transformer)
  private final val WORKERS: String = "workers"
  private var morePayloadAvailable: Boolean = true

//  supervisorState.setWorkers(workers)
//  supervisorState.setTransformerId(transformer.id)
//  supervisorState.setTransformerName(transformer.name)
  startWorkers()

  def getStatus: SupervisorState = {
    supervisorState
  }

  def onReceive(message: Any) {
    message match {
      case wr: WorkerResult =>
        handleReceivedWorkResult(wr)
      case command: SupervisorCommand =>
        handleSupervisorCommand(command)
      case str: String =>
        Logger.debug("Received message: " + message + " My status: " + supervisorState.getStatus.toString)
      case _ =>
        Logger.debug("Do not understand")
    }
  }

  private def handleSupervisorCommand(command: SupervisorCommand) {
    command.getStatus match {
      case SupervisorCommandType.START =>
        supervisorState.setStatus(SupervisorStateType.STARTING)
        sendMessageToInformer("Start request received")
        supervisorState.resetTimeOutCount()
        startWorkers()
      case SupervisorCommandType.PAUSE =>
        if (supervisorState.getStatus eq SupervisorStateType.RUNNING) {
          supervisorState.setStatus(SupervisorStateType.PAUSING)
          sendMessageToInformer("Pause request received")
        } else if (supervisorState.getStatus eq SupervisorStateType.PAUSED) {
          supervisorState.setStatus(SupervisorStateType.STARTING)
          supervisorState.resetTimeOutCount()
          sendMessageToInformer("Resume request received")
          startWorkers()
        } else
          sendMessageToInformer("Unable to pause/resume")
      case SupervisorCommandType.RESUME =>
        supervisorState.setStatus(SupervisorStateType.RESUMING)
      case SupervisorCommandType.STOP =>
        supervisorState.setStatus(SupervisorStateType.STOPPING)
      case SupervisorCommandType.REPORT =>
        sendMessageToInformer("")
      case _ =>
        sendMessageToInformer("Unhandled command:" + command.getStatus)
    }
  }

  private def handleReceivedWorkResult(wr: WorkerResult) {
    wr.getStatus match {
      case WorkerResultStatus.READY =>
        supervisorState.incrementActiveWorkers()
        if (supervisorState.getActiveWorkers == supervisorState.getWorkers) {
          supervisorState.setStatus(SupervisorStateType.RUNNING)
          sendMessageToInformer("All workers have reported for duty.")
        }
        Logger.trace(self.toString + " - reported for duty")
        sendNewPayLoad(getSender())
      case WorkerResultStatus.DONE =>
        if (supervisorState.getStatus eq SupervisorStateType.STARTING) {
          supervisorState.setStatus(SupervisorStateType.RUNNING)
        }
        supervisorState.incrementSuccesCount()
        Logger.trace("[" + supervisorState.getActiveWorkers + "] " + sender.toString + ": " + wr.getResult + " (" + wr.getProcessingTime + "ms)")
        if ((supervisorState.getSuccesCount % 1000) == 0 && supervisorState.getSuccesCount != 0) {
          sendMessageToInformer(self.toString + " - Did another 1000")
          Logger.debug(" [" + supervisorState.getActiveWorkers + "] " + self.toString + " - Success count: " + supervisorState.getSuccesCount)
        }
        sendNewPayLoad(getSender())
      case WorkerResultStatus.FAILED =>
        if (supervisorState.getStatus eq SupervisorStateType.STARTING) {
          supervisorState.setStatus(SupervisorStateType.RUNNING)
        }
        supervisorState.incrementFailureCount()
        Logger.debug("[" + supervisorState.getActiveWorkers + "] " + self.toString + " - Failure count: " + supervisorState.getFailureCount + " - " + wr.getResult)
        writeToErrorFile(wr.getFailedInput)
        sendMessageToInformer("Error: " + wr.getResult)
        sendNewPayLoad(getSender())
      case WorkerResultStatus.TIMEOUT =>
        if (supervisorState.getStatus eq SupervisorStateType.STARTING) {
          supervisorState.setStatus(SupervisorStateType.RUNNING)
        }
        supervisorState.incrementTimeOutCount()
        Logger.debug("[" + supervisorState.getActiveWorkers + "] " + self.toString + " - Time-out - " + wr.getResult)
        if (supervisorState.getTimeOutcount < MAX_TIMEOUTS) {
          sendMessageToInformer("Time-out " + wr.getResult)
          val retry: Payload = new Payload(transformer.name, wr.getLineNumber, wr.getFailedInput)
          getSender().tell(retry, getSelf())
        }
        else {
          if (supervisorState.getStatus ne SupervisorStateType.PAUSING) {
            supervisorState.setStatus(SupervisorStateType.PAUSING)
            sendMessageToInformer("To many time-outs, going to pause")
          }
          writeToErrorFile(wr.getFailedInput)
          getSender().tell(PoisonPill.getInstance, self)
        }
      case WorkerResultStatus.SUICIDE =>
        Logger.debug("Worker " + sender.path + " committed suicide")
        supervisorState.decrementActiveWorkers()
        if (supervisorState.getActiveWorkers == 0) {
          supervisorState.setStatus(SupervisorStateType.STOPPED)
          sendMessageToInformer("All workers stopped")
          Logger.info(self.toString + " - All workers stopped.")
        }
      case _ =>

    }
  }

  private def sendNewPayLoad(actor: ActorRef) {
    if ((supervisorState.getStatus ne SupervisorStateType.STARTING) && (supervisorState.getStatus ne SupervisorStateType.RESUMING) && (supervisorState.getStatus ne SupervisorStateType.RUNNING)) {
      if (supervisorState.getStatus eq SupervisorStateType.STOPPING) {
        supervisorState.setStatus(SupervisorStateType.STOPPED)
        supervisorState.setStopTime(new DateTime)
      }
      if (supervisorState.getStatus eq SupervisorStateType.PAUSING) {
        supervisorState.setStatus(SupervisorStateType.PAUSED)
      }
      getSender().tell(PoisonPill.getInstance, self)
    }
    if (morePayloadAvailable) {
      val payload: Payload = getNextPayload
      Logger.trace(self.toString + " - Got new payload")
      if (payload != null) {
        if (supervisorState.getFailureCount == 0 && supervisorState.getSuccesCount == 0) {
          supervisorState.setCurrentFileSpecs(fileImporter.getCurrentFileName, fileImporter.getNrOfLines)
        }
        Logger.trace(self.toString + " - Sending payload to " + actor.toString)
        actor.tell(payload, getSelf())
        return
      }
    }
    getSender().tell(PoisonPill.getInstance, self)
  }

  private def sendMessageToInformer(message: String) {
    Informer.getInstance.sendMessage(supervisorState, message)
  }

  private def writeToErrorFile(line: String) {
    try {
      val file: File = new File(String.format("%s.%s", supervisorState.getCurrentFile.substring(0, supervisorState.getCurrentFile.lastIndexOf(".")), "errors"))
      if (!file.exists) {
        file.createNewFile
      }
      val writer: OutputStreamWriter = new OutputStreamWriter(new FileOutputStream(file.getAbsolutePath, true), transformer.importFilecontentType)
      val bw: BufferedWriter = new BufferedWriter(writer)
      bw.write(line)
      bw.newLine()
      bw.flush()
      bw.close()
    }
    catch {
      case e: Exception =>
        Logger.error("Problem writing to error file. Cause: " + e.getMessage)
        sendMessageToInformer("Problem writing to error file. Cause: " + e.getMessage)
        supervisorState.setStatus(SupervisorStateType.PAUSING)
    }
  }

  private def getNextPayload: Payload = {
    val line: String = fileImporter.getNextLine
    if (line != null) {
      supervisorState.incrementPayloadCount()
      new Payload(transformer.name, supervisorState.getPayloadCount, line)
    }
    else {
      sendMessageToInformer("All available lines queued, nothing more to do")
      Logger.debug("All available lines queued, nothing more to do")
      morePayloadAvailable = false
      null
    }
  }

  private def startWorkers() {
    val router: ActorRef = getContext().actorOf(new Props(new UntypedActorFactory {
      def create(): UntypedActor = {
        new WebserviceWorkerActor(self, transformer)
      }
    }).withRouter(new RoundRobinRouter(supervisorState.getWorkers)), WORKERS)
    sendMessageToInformer("Starting workers")
  }

  override def preStart() {
    supervisorState.setStartTime(new DateTime)
    supervisorState.setStatus(SupervisorStateType.STARTING)
    sendMessageToInformer("Supervisor ready")
    Logger.info(self.toString + " - Supervisor ready")
  }

  override def postStop() {
    val endTime: DateTime = new DateTime
    supervisorState.setStatus(SupervisorStateType.STOPPED)
    supervisorState.setStopTime(endTime)
    val spendTime: String = Days.daysBetween(supervisorState.getStartTime, endTime).getDays + " days, " + Hours.hoursBetween(supervisorState.getStartTime, endTime).getHours % 24 + " hours, " + Minutes.minutesBetween(supervisorState.getStartTime, endTime).getMinutes % 60 + " minutes, " + Seconds.secondsBetween(supervisorState.getStartTime, endTime).getSeconds % 60 + " seconds."
    sendMessageToInformer("Stopped supervisor. Time spend: " + spendTime)
    Logger.info(self.toString + " - Stopping supervisor. Time spend: " + spendTime)
  }


}