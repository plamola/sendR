package support.bulkImport

import scala.xml.{XML, NodeSeq, Node, Elem}
import scala.xml.transform.{RewriteRule, RuleTransformer}

/**
 * Author: matthijs 
 * Created on: 05 Mar 2014.
 */
object SOAPCreator {

  def translate(template : String ) : String  =
    myRule.transform(XML.loadString(template)).toString()

  class RemoveEmptyTagsRule extends RewriteRule {
    override def transform(n: Node) =
      n match {
        case e @ Elem(prefix, label, attributes, scope, child @ _*) if
          isEmptyElement(e) => NodeSeq.Empty
        case other => other
      }
  }

  val myRule = new RuleTransformer(new RemoveEmptyTagsRule)

  private def isEmptyElement(n: Node): Boolean =
    n match {
      case e @ Elem(prefix, label, attributes, scope, child @ _*) if
        (e.text.isEmpty &&
        (e.attributes.isEmpty || e.attributes.forall(_.value == null))
        && e.child.isEmpty) => true
      case other => false
    }


}
