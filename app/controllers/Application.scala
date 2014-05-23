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
    )(LoginData.apply)(LoginData.unapply) verifying("password",
      fields => fields match { case loginData => validate(loginData.email, loginData.password).isDefined
      }
      )
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
          BadRequest(views.html.login(formWithErrors))//.flashing("error" -> "Enter a e-mail address and a password")
        },
        login =>
          //if (User.authenticate(login.email, login.password))
            Redirect(routes.Application.index()).withSession("email" -> login.email).flashing("success" -> "Login succesful")
        //else
            //BadRequest(views.html.login(loginForm)).flashing("error" -> "Incorrect email address/password combination")
      )
  }

  /**
   * Logout and clean the session.
   */
  def logout = Action {
    implicit request =>
      Redirect(routes.Application.login()).withNewSession.flashing("success" -> "You've been logged out")
  }


  def validate(email: String, password: String)  = {
    if (User.authenticate(email, password))
      Some(LoginData(email, password))
    else
      None
  }
}
  case class LoginData(email: String, password: String) {


  }

