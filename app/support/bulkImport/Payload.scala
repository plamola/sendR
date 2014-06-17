package support.bulkImport

import org.joda.time.DateTime

/**
 * Created with IntelliJ IDEA.
 * User: matthijs
 * Date: 6/30/13
 * Time: 11:25 AM
 * To change this template use File | Settings | File Templates.
 */
class Payload(lineNumber: Long, line: String, sourceFileName : String) {

  private val creationDate : DateTime = new DateTime

  def getLine: String = line

  def getLineNumber: Long = lineNumber

  def getCreationDate : DateTime = creationDate

  def getSourceFileName = sourceFileName

}