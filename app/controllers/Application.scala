package controllers

import models.User
import play.api.mvc.{Session, Action, Controller}
import play.api.data._
import play.api.data.Forms._

object Application extends Controller {


  val loginForm = Form(
    mapping(
      "email" -> nonEmptyText,
      "password" -> nonEmptyText
    )(LoginData.apply)(LoginData.unapply)
  )

  /**
   * Display the home page.
   */
  def index = Action {
    implicit request =>
      Ok(views.html.index())
  }

  /**
   * Login page.
   */
  def login = Action {
    implicit request =>
    Ok(views.html.login(loginForm))
  }

  /**
   * Handle login form submission.
   */
  def authenticate = Action {
    implicit request =>
      loginForm.bindFromRequest.fold(
        formWithErrors => {
          BadRequest(views.html.login(formWithErrors))
        },
        login => {
          Redirect(routes.Application.index()).withSession("email" -> login.email)
        }
      )
  }

  /**
   * Logout and clean the session.
   */
  def logout = Action {
    implicit request =>
      Redirect(routes.Application.login()).withNewSession.flashing("success" -> "You've been logged out")
  }

}
  case class LoginData(email: String, password: String) {

    def validate: String = {
      if (!User.authenticate(email, password)) {
        "Invalid user or password"
      }
      null
    }

  }

