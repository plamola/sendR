package controllers

import play.api.libs.EventSource
import play.api.libs.iteratee.{Enumeratee, Concurrent}
import play.api.libs.json.JsValue
import play.api.mvc.{Action, Controller}
import play.api.libs.concurrent.Execution.Implicits._


object ServerSendEvents extends Controller {

  /** Central hub for distributing update messages */
  val (outputMessage, outputChannel) = Concurrent.broadcast[JsValue]

  /** Controller action serving activity based on channel */
  def statusFeedAll() =
    Ok.chunked(outputMessage  &> Concurrent.buffer(20) &> EventSource()).as("text/event-stream")


}