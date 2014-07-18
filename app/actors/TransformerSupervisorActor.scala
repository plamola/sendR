package actors

import akka.actor._
import akka.routing.RoundRobinRouter
import models.Transformer
import org.joda.time._
import play.Logger
import support.FileHelper
import support.Informer
import java.io._
import support.bulkImport._

class TransformerSupervisorActor(workers: Int, transformer: Transformer) extends UntypedActor {

  private final val MAX_TIMEOUTS: Int = 100
  private final val WORKERS: String = "workers"

  private val supervisorState: SupervisorState = new SupervisorState(workers, transformer)
  private var fileReaderActor : ActorRef = null//startWithNewFile()

  def getStatus: SupervisorState = {
    supervisorState
  }

  def onReceive(message: Any) {
    message match {
      case wr: WorkerResult =>
        handleReceivedWorkResult(wr)

      case command: SupervisorCommand =>
        handleSupervisorCommand(command)

      case fr : FileReaderStatus =>
        handleFileReaderStatus(fr)

      case str: String =>
        Logger.debug("Received message: " + message + " My status: " + supervisorState.getStatus.toString)

      case _ =>
        Logger.debug("Do not understand")
    }
  }

  private def handleFileReaderStatus(fr: FileReaderStatus) {
    fr.status match {
      case FileReaderStatusType.READY =>
        startWorkers()

      case FileReaderStatusType.SUICIDE =>
        fileReaderActor = startWithNewFile()

      case FileReaderStatusType.NO_WORK =>

    }
  }

  private def handleSupervisorCommand(command: SupervisorCommand) {
    command.getStatus match {
      case SupervisorCommandType.START =>
        supervisorState.setStatus(SupervisorStateType.STARTING)
        sendMessageToInformer("Start request received")
        supervisorState.resetTimeOutCount()
        if (fileReaderActor != null) {
          startWorkers()
        } else {
          fileReaderActor = startWithNewFile()
        }

      case SupervisorCommandType.PAUSE =>
        if (supervisorState.getStatus eq SupervisorStateType.RUNNING) {
          supervisorState.setStatus(SupervisorStateType.PAUSING)
          sendMessageToInformer("Pause request received")
        } else if (supervisorState.getStatus eq SupervisorStateType.PAUSED) {
          supervisorState.setStatus(SupervisorStateType.STARTING)
          supervisorState.resetTimeOutCount()
          sendMessageToInformer("Resume request received")
          if (fileReaderActor != null) {
            startWorkers()
          } else {
            fileReaderActor = startWithNewFile()
            startWorkers()
          }
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
    if (supervisorState.getStatus eq SupervisorStateType.STARTING) {
      supervisorState.setStatus(SupervisorStateType.RUNNING)
    }

      wr.status match {
        case WorkerResultStatus.FAILED =>
          supervisorState.incrementFailureCount()
          Logger.debug("[" + supervisorState.getActiveWorkers + "] " + self.toString + " - Failure count: " + supervisorState.getFailureCount + " - " + wr.getResult)
          wr.getPayLoad match {
            case Some(payload) => writeToErrorFile(payload)
            case None => Logger.error("Received 'FAILED' without payload")
          }
          sendMessageToInformer("Error: " + wr.getResult)
          fileReaderActor.tell("give him something else",getSender())

        case WorkerResultStatus.TIMEOUT =>
          supervisorState.incrementTimeOutCount()
          Logger.debug("[" + supervisorState.getActiveWorkers + "] " + self.toString + " - Time-out - " + wr.getResult)
          if ((supervisorState.getTimeOutcount < MAX_TIMEOUTS) && (supervisorState.getStatus ne SupervisorStateType.PAUSING)) {
            sendMessageToInformer("Time-out " + wr.getResult)
            wr.getPayLoad match {
              case Some(payload) => getSender() ! new Payload(payload.getLineNumber, payload.getLine, payload.getSourceFileName)
              case None => Logger.error("Received 'TIMEOUT' without payload")
            }
          }
          else {
            if (supervisorState.getStatus ne SupervisorStateType.PAUSING) {
              supervisorState.setStatus(SupervisorStateType.PAUSING)
              sendMessageToInformer("To many time-outs, going to pause")
            }
            supervisorState.incrementFailureCount()
            wr.getPayLoad match {
              case Some(payload) => writeToErrorFile(payload)
              case None => Logger.error("Received 'TIMEOUT' without payload")
            }
            getSender() ! PoisonPill.getInstance
          }

        case WorkerResultStatus.READY =>
          supervisorState.incrementActiveWorkers()
          if (supervisorState.getActiveWorkers == supervisorState.getWorkers) {
            supervisorState.setStatus(SupervisorStateType.RUNNING)
          }
          Logger.trace(self.toString + " - reported for duty")
          fileReaderActor.tell("give hime something to do",getSender())

        case WorkerResultStatus.DONE =>
          supervisorState.incrementSuccesCount()
          Logger.trace("[" + supervisorState.getActiveWorkers + "] " + sender.toString + ": " + wr.getResult + " (" + wr.getProcessingTime + "ms)")
          if ((supervisorState.getSuccesCount % 1000) == 0 && supervisorState.getSuccesCount != 0) {
            sendMessageToInformer(self.toString + " - Did another 1000")
            Logger.debug(" [" + supervisorState.getActiveWorkers + "] " + self.toString + " - Success count: " + supervisorState.getSuccesCount)
          }
          fileReaderActor.tell("give hime some more",getSender())

        case WorkerResultStatus.NO_WORK =>

          Logger.debug("Killing worker " + getSender().toString())
          getSender ! PoisonPill.getInstance

        case WorkerResultStatus.SUICIDE =>
          supervisorState.decrementActiveWorkers()
          Logger.debug("Worker " + sender.path + " committed suicide [" + supervisorState.getActiveWorkers + "/" + workers + "]")
          if (supervisorState.getActiveWorkers == 0) {
            //sendMessageToInformer("All workers stopped")
            Logger.info(self.toString + " - All workers stopped.")
            if (supervisorState.getStatus eq SupervisorStateType.STOPPING) {
              if (fileReaderActor != null) {
                fileReaderActor ! PoisonPill.getInstance
                if(supervisorState.getNrOfLines == ( supervisorState.getSuccesCount + supervisorState.getFailureCount)) {
                  sendMessageToInformer("Finished with file ")
                }
              }
              supervisorState.setStatus(SupervisorStateType.STOPPED)
            }
            if (supervisorState.getStatus eq SupervisorStateType.PAUSING)
              supervisorState.setStatus(SupervisorStateType.PAUSED)
            if (supervisorState.getStatus eq SupervisorStateType.RUNNING) {
              // All workers died while status running -> file must be empty -> kill filereader actor
              if (fileReaderActor != null)
                fileReaderActor ! PoisonPill.getInstance
            }
          }


        case _ =>
      }
  }

  private def startWithNewFile() : ActorRef = {
    FileHelper.findNextFile(transformer.importPath,transformer.importFileExtension) match {
      case Some(file) =>
        Logger.debug("Found: " + file.fullPath + " [" + file.nrOfLines+ " lines]")
        sendMessageToInformer("Found: " + file.fullPath + " [" + file.nrOfLines+ " lines]")
        supervisorState.setCurrentFileSpecs(file.fullPath, file.nrOfLines)
        if (getContext().getChild("file-reader") == null) {
            Logger.debug("Create file-reader")
            getContext().actorOf(Props(new FileReaderActor(self,file.fullPath,transformer.importFilecontentType)),"file-reader")
        } else {
          Logger.debug("Re-use file-reader")
          getContext().getChild("file-reader")
        }
      case None =>
        supervisorState.setStatus(SupervisorStateType.STOPPED)
        sendMessageToInformer("No files found.")
        Logger.debug("No files found.")
        null
    }
  }

  private def startWorkers() {
    getContext().actorOf(Props(new TransformerWorkerActor(self, transformer))
      .withRouter(new RoundRobinRouter(supervisorState.getWorkers)), WORKERS)
    sendMessageToInformer("Starting workers")
  }

  private def sendMessageToInformer(message: String) {
    Informer.getInstance.sendMessage(supervisorState, message)
  }

  private def writeToErrorFile(payload: Payload) {
    try {
      val file: File = new File(String.format("%s.%s", payload.getSourceFileName.substring(0, payload.getSourceFileName.lastIndexOf(".")), "errors"))
      if (!file.exists) {
        file.createNewFile
      }
      val writer: OutputStreamWriter = new OutputStreamWriter(new FileOutputStream(file.getAbsolutePath, true), transformer.importFilecontentType)
      val bw: BufferedWriter = new BufferedWriter(writer)
      bw.write(payload.getLine)
      bw.newLine()
      bw.flush()
      bw.close()
    }
    catch {
      case e: Exception =>
        Logger.error("Problem writing to error file. Reason: " + e.getMessage)
        sendMessageToInformer("Problem writing to error file. Reason: " + e.getMessage)
        supervisorState.setStatus(SupervisorStateType.PAUSING)
    }
  }

  override def preStart() {
    fileReaderActor = startWithNewFile()
    if (fileReaderActor != null) {
      supervisorState.setStartTime(new DateTime)
      supervisorState.setStatus(SupervisorStateType.STARTING)
      Logger.info(self.toString + " - Supervisor ready")
    } else {
      supervisorState.setStatus(SupervisorStateType.STOPPED)
    }

  }

  override def postStop() {
    val endTime: DateTime = new DateTime
    supervisorState.setStatus(SupervisorStateType.STOPPED)
    supervisorState.setStopTime(endTime)
    val spendTime: String = Days.daysBetween(supervisorState.getStartTime, endTime).getDays + " days, " + Hours.hoursBetween(supervisorState.getStartTime, endTime).getHours % 24 + " hours, " + Minutes.minutesBetween(supervisorState.getStartTime, endTime).getMinutes % 60 + " minutes, " + Seconds.secondsBetween(supervisorState.getStartTime, endTime).getSeconds % 60 + " seconds."
    sendMessageToInformer("Stopped supervisor. Time spend: " + spendTime)
    Logger.info(self.toString + " - Stopped supervisor. Time spend: " + spendTime)
  }

}