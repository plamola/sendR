package support.bulkImport

/**
 * Created with IntelliJ IDEA.
 * User: matthijs
 * Date: 6/30/13
 * Time: 11:25 AM
 * To change this template use File | Settings | File Templates.
 */
class Payload(transformerName: String, lineNumber: Long, line: String) {

  def getTransformerName: String = {
    transformerName
  }

  def getLine: String = {
    line
  }

  def getLineNumber: Long = {
    lineNumber
  }

}