package support

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.Logger
import java.io._
import java.util.Comparator
import java.util.Date
import scala.io.Source

object FileHelper {

  def findNextFile(importDirectory : String, fileExtension : String) : Option[FileDetails] = {
    if (importDirectory == null) {
      Logger.warn("The import directory is incorrect.")
      None
    }
    val folder: File = new File(importDirectory.replace("\\", "\\\\"))
    val files: Array[File] = folder.listFiles
    if (files == null) {
      Logger.warn("No files found. Does directory " + folder + " exist and does it contain a file with the extension " + fileExtension + " ?")
      None
    }
    java.util.Arrays.sort(files, new Comparator[File] {
      def compare(f1: File, f2: File): Int = {
        f1.lastModified.compareTo(f2.lastModified)
      }
    })

    // Get a list of files which have the correct file extension
    val fileList : Array[File] = {
      for {importFile <- files if importFile.isFile && importFile.getName.toLowerCase.endsWith(fileExtension.toLowerCase)}
      yield importFile
    }
    if (fileList.length > 0 )
      Some(FileDetails(fileList.head.getName, fileList.head.getAbsolutePath, countLinesInFile(fileList.head.getAbsolutePath)))
    else
      None
  }


  /**
   * Count the number of lines in the text file
   * @param fileName  path of the file
   * @return          number of lines in the file
   */
  private def countLinesInFile(fileName: String): Long = {
    var newlineCount = 0L
    for (line <- Source.fromFile(fileName).getLines()) {
      newlineCount += 1
    }
    newlineCount
  }


  /**
   * Change the file extension
   *
   * @param file          Name of the file
   * @param extension     The new file extension
   * @return              File reference of the renamed file
   * @throws Exception    Failed to rename
   */
  def changeFileExtension(file: File, extension: String): File = {
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