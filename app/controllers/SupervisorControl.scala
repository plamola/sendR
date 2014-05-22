package controllers

import models.Transformer
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import support.bulkImport.ImportMangerSystemSingleton
import support.bulkImport.ImportMangerSystem
import play.api.{Play, Logger}
import play.api.Play.current

/**
 * Created with IntelliJ IDEA.
 * User: matthijs
 * Date: 7/16/13
 * Time: 8:38 PM
 * To change this template use File | Settings | File Templates.
 */

case class TransformerData(
                            id: Long,
                            name: String,
                            category: String,
                            importPath: String,
                            importFileExtension: String,
                            importFilecontentType: String,
                            webserviceCharSet: String,
                            webserviceURL: String,
                            webserviceUser: String,
                            webservicePassword: String,
                            webserviceTimeout: Int,
                            webserviceTemplate: String,
                            timeStampString: String)


object SupervisorControl extends Controller with Secured {

  def start(id: Long) = Action {
    implicit request =>
      Transformer.findById(id) match {
        case Some(tr) =>
          val mgr: ImportMangerSystem = ImportMangerSystemSingleton.getInstance
          mgr.startImportManager(getNumberOfWorkers, tr)
        case None =>
          Logger.error("Transformer with id " + id + " does not exist.")
      }
      Ok
  }

  def pause(id: Long) = Action {
    implicit request =>
      if (checkIfTransformerExists(id)) {
        Logger.debug("Contoller: Pause/Resume ImportManager for " + id)
        val mgr: ImportMangerSystem = ImportMangerSystemSingleton.getInstance
        mgr.pauseImportManager(id)
      }
      else {
        Logger.error("Transformer with id " + id + " does not exist.")
      }
      Ok
  }

  def stop(id: Long) = Action {
    implicit request =>
      if (checkIfTransformerExists(id)) {
        Logger.debug("Contoller: Stopping ImportManager for " + id)
        val mgr: ImportMangerSystem = ImportMangerSystemSingleton.getInstance
        mgr.stopImportManager(id)
      }
      else {
        Logger.error("Transformer with id " + id + " does not exist.")
      }
      Ok
  }

  def edit(id: Long) = Action {
    implicit request =>
      if (id == 0) {
        Ok(views.html.transformer_newedit
          .render("New transformer", id, transformerForm.fill(
              new TransformerData(0L, null, null, null, ".csv", "cp1252", "UTF-8", null, null, null, 10000,
                "<soap></soap>", "2014-01-01T00:00:00Z")
        ),session
        )
        )
      }
      else {
        Ok(views.html.transformer_newedit
          .render("Edit transformer", id, transformerForm.fill(TransformerToTransformerData(Transformer.findById(id).get)),session)
        )
      }
  }

  def save(id: Long) = Action {
    implicit request =>
      transformerForm.bindFromRequest.fold(
        formWithErrors => {
          BadRequest(views.html.transformer_newedit.render("Error while saving", id, formWithErrors,session))
        },
        transformerData => {
          try {
            if (id.longValue == 0L) {
              Transformer.create(TransformerDataToTransformer(transformerData))
            }
            else {
              Transformer.update(TransformerDataToTransformer(transformerData))
            }
            Redirect(routes.Application.index())
          }
          catch {
            case e: Exception =>
              BadRequest(views.html.transformer_newedit.render("Error while saving. " + e.getMessage, id, transformerForm.fill(transformerData),session))
          }
        }
      )
  }

  def delete(id: Long) = Action {
    implicit request =>
      Transformer.delete(id)
      Redirect(routes.Application.index())
  }

  def cloneThisTransformer(sourceId: Long) = Action {
    implicit request =>
      Transformer.cloneTransformer(sourceId) match {
        case Some(cloned) =>
          Redirect(routes.SupervisorControl.edit(cloned.id))
        case None =>
          Redirect(routes.Application.index()).flashing("error" -> "Cloning of transformer failed")
      }
  }

  private def checkIfTransformerExists(id: Long): Boolean = {
    Transformer.findById(id).nonEmpty
  }

  private def getNumberOfWorkers: Int = {
    Play.application.configuration.getString("sendr.nrofworkers") match {
      case Some(nrOfWorkers) =>
        try {
          Integer.parseInt(nrOfWorkers)
        }
        catch {
          case e: Exception => 8
        }
      case None => 8
    }
  }

  private def TransformerDataToTransformer(tr: TransformerData) = new
    Transformer(tr.id, tr.name, tr.category, tr.importPath, tr.importFileExtension, tr.importFilecontentType, tr.webserviceCharSet,
      tr.webserviceURL, tr.webserviceUser, tr.webservicePassword, tr.webserviceTimeout, tr.webserviceTemplate, tr.timeStampString, 1)


  private def TransformerToTransformerData(tr: Transformer) = new
      TransformerData(tr.id, tr.name, tr.category, tr.importPath, tr.importFileExtension, tr.importFilecontentType, tr.webserviceCharSet,
        tr.webserviceURL, tr.webserviceUser, tr.webservicePassword, tr.webserviceTimeout, tr.webserviceTemplate, tr.timeStampString)




  val transformerForm = Form(
    mapping(
      "id" -> longNumber,
      "name" -> nonEmptyText,
      "category" -> text,
      "importPath" -> text,
      "importFileExtension" -> text,
      "importFilecontentType" -> text,
      "webserviceCharSet" -> text,
      "webserviceURL" -> text,
      "webserviceUser" -> text,
      "webservicePassword" -> text,
      "webserviceTimeout" -> number,
      "webserviceTemplate" -> text,
      "timeStampString" -> text
    )(TransformerData.apply)(TransformerData.unapply)
  )

}