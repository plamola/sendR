package actors

import akka.actor.{UntypedActor, ActorRef}
import play.Logger
import support.bulkImport._
import java.io._
import org.joda.time.DateTime
import support.FileHelper


class FileReaderActor(mySupervisor: ActorRef, filePath : String, fileContentType: String) extends UntypedActor  {

  private var currentFile: File = null
  private var bf: BufferedReader = null
  private var in: DataInputStream = null
  private var lineNumber: Long = 0


  def incrementLineNumberCount() : Long = synchronized {
    lineNumber += 1
    lineNumber
  }

  override def preStart() {
    Logger.debug(self.toString + " - Starting filereader")
    currentFile = FileHelper.changeFileExtension(new File(filePath), "busy_" + new DateTime().toString("yyyyMMdd-HHmmss"))
    openFile()
    if (mySupervisor != null)
      mySupervisor ! new FileReaderStatus(FileReaderStatusType.READY)
  }

  override def postStop() {
    closeFile()
    // TODO If file has not been completed, rename to 'stopped_'
    FileHelper.changeFileExtension(currentFile, "imported_" + new DateTime().toString("yyyyMMdd-HHmmss"))
    Logger.debug(self.toString + " - Terminated filereader ")
    mySupervisor ! new FileReaderStatus(FileReaderStatusType.SUICIDE)
  }

  def onReceive(message: Any) {
    message match {
      case str: String =>
        getNextLine match {
          case Some(line) =>
            sender ! new Payload(incrementLineNumberCount(), line, currentFile.getAbsolutePath)
          case None =>
            sender ! new FileReaderStatus(FileReaderStatusType.NO_WORK)
            Logger.debug("Out of work - no more file: " + sender.toString())
        }
      case _ =>
        Logger.debug("Unknown message type from " + sender.toString())
    }
  }


  /**
   * Open the file
   * @throws Exception    file could not be opened
   */
  private def openFile() {
    try {
      val fr: FileInputStream = new FileInputStream(currentFile.getAbsolutePath)
      in = new DataInputStream(fr)
      bf = new BufferedReader(new InputStreamReader(in, fileContentType))
    }
    catch {
      case e: Exception =>
        closeFile()
        throw new Exception(String.format("%s -> %s", e.getStackTrace.head.getMethodName, e.getMessage))
    }
  }


  /**
   * Close the file
   */
  private def closeFile() {
    lineNumber = 0
    try {
      if (in != null) in.close()
      if (bf != null) bf.close()
    }
    catch {
      case e: IOException =>
        Logger.error("Failed to closeFile" + e.getMessage)
    }
  }

  /**
   * Reads lines from all the files in the importPath
   * When finished reading a file, it will be renamed
   *
   * @return the next line of the file, or null in case of an error / EOF
   */
  def getNextLine: Option[String] = {
    try {
      val line: String = bf.readLine
      if (line != null)
        Some(line)
      else
        None
    }
    catch {
      case e: Exception =>
        // TODO stop the actor?
        Logger.error(String.format("%s -> %s", e.getStackTrace.head.getMethodName, e.getMessage))
        None
    }
  }



}
