package support

import models.Transformer
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.Logger
import java.io._
import java.util.Comparator
import java.util.Date
import scala.io.Source

class FileImporter(transformer: Transformer) {

  private val EXTENSION: String = transformer.importFileExtension
  private val contentTypeFile: String = transformer.importFilecontentType
  private var bf: BufferedReader = null
  private var in: DataInputStream = null
  private var fileIsOpen: Boolean = false
  private var currentFile: File = null
  private var lineNumber: Long = 0
  private var numberOfLinesInFile: Long = 0


  def getNrOfLines: Long = {
    numberOfLinesInFile
  }

  def getCurrentFileName: String = {
    currentFile.getAbsolutePath
  }

  /**
   * Change the file extension
   *
   * @param file          Name of the file
   * @param extension     The new file extension
   * @return              File reference of the renamed file
   * @throws Exception    Failed to rename
   */
  private def changeFileExtension(file: File, extension: String): File = {
    try {
      val newfile: File = new File(String.format("%s.%s", file.getAbsolutePath.substring(0, file.getAbsolutePath.lastIndexOf(".")), extension))
      if (!file.renameTo(newfile)) {
        throw new Exception("Rename failed.")
      }
      else newfile
    }
    catch {
      case e: Exception =>
        throw new Exception(String.format("%s -> %s", e.getStackTrace.head.getMethodName, e.getMessage))
    }
  }


  /**
   * Reads lines from all the files in the importPath
   * When finished reading a file, it will be renamed
   *
   * @return the next line of the file, or null in case of an error / EOF
   */
  def getNextLine: String = {
    if (!fileIsOpen) {
      currentFile = getNextFile//(transformer.importPath)
      if (currentFile == null) {
        Logger.debug("No next file")
        return null
      }
      try {
        openFile(currentFile.getAbsolutePath)
        if (!fileIsOpen) {
          Logger.debug("Could not open file")
          return null
        }
      }
      catch {
        case e: Exception =>
          Logger.debug("Could not open file")
          return null
      }
    }
    try {
      val line: String = bf.readLine
      if (line != null) {
        lineNumber += 1
        Logger.trace(lineNumber + "/" + numberOfLinesInFile)
        line
      }
      else {
        closeFile()
        val date: String = new DateTime().toString("yyyyMMdd-HHmmss")
        currentFile = changeFileExtension(currentFile, "imported_" + date)
        getNextLine
      }
    }
    catch {
      case e: Exception =>
        Logger.error(String.format("%s -> %s", e.getStackTrace.head.getMethodName, e.getMessage))
        null
    }
  }

  /**
   * Opens the next file in the directory with the correct file extension
   * The opened file is renamed, to prevent another process from opening it.
   *
   * @return                  File found in the directory
   */
  private def getNextFile: File = {
    val importDirectory = transformer.importPath
    if (importDirectory == null) {
      Logger.warn("The import directory is incorrect.")
      return null
    }
    val folder: File = new File(importDirectory.replace("\\", "\\\\"))
    val files: Array[File] = folder.listFiles
    if (files == null) {
      Logger.warn("No files found. Does directory " + folder + " exist and does it contain a file with the extension " + EXTENSION + " ?")
      return null
    }
    java.util.Arrays.sort(files, new Comparator[File] {
      def compare(f1: File, f2: File): Int = {
        f1.lastModified.compareTo(f2.lastModified)
      }
    })
    for (file1 <- files) {
      if (file1.isFile) {
        if (file1.getName.toLowerCase.endsWith(EXTENSION.toLowerCase)) {
          var file: File = file1
          try {
            val date: String = new DateTime().toString("yyyyMMdd-HHmmss")
            file = changeFileExtension(file, "busy_" + date)
            numberOfLinesInFile = countLinesInFile(file.getAbsolutePath)
            openFile(file.getAbsolutePath)
            lineNumber = 0
            return file
          }
          catch {
            case e: Exception =>
              Logger.error("Failed to rename " + file.getAbsolutePath)
              return null
          }
        }
      }
    }
    null
  }

  /**
   * Count the number of lines in the text file
   * @param fileName  path of the file
   * @return          number of lines in the file
   */
  private def countLinesInFile(fileName: String): Long = {
    Logger.debug("Counting lines in " + fileName)
    val source = Source.fromFile(fileName)
    var newlineCount = 0L
    for (line <- source.getLines()) {
      newlineCount += 1
    }
    Logger.debug("File " + fileName + " contains " + newlineCount + " lines.")
    newlineCount
  }

  /**
   * Open the file
   *
   * @param filePath      file path of the file to be opened
   * @throws Exception    file could not be opened
   */
  private def openFile(filePath: String) {
    try {
      val fr: FileInputStream = new FileInputStream(filePath)
      in = new DataInputStream(fr)
      bf = new BufferedReader(new InputStreamReader(in, contentTypeFile))
      fileIsOpen = true
    }
    catch {
      case e: Exception =>
        try {
          if (in != null) in.close()
          if (bf != null) bf.close()
        }
        catch {
          case e1: IOException =>
            e1.printStackTrace()
        }
        throw new Exception(String.format("%s -> %s", e.getStackTrace.head.getMethodName, e.getMessage))
    }
  }

  /**
   * Close the file
   */
  private def closeFile() {
    fileIsOpen = false
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
   * Converts a timestamp string to a Java Date
   *
   * format of the string: 2013-03-08-09.39.20.264000
   *
   * @param value   String containing a timestamp
   * @return        Date object with the converted timestamp string
   */
  protected def convertStringToDate(value: String): Date = {
    DateTime.parse(value, DateTimeFormat.forPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS")).toDate
  }


}