package models

import play.api.db._
import play.api.Play.current
import anorm._
import anorm.SqlParser._


case class User(email: String, password: String) {
  override def toString: String = {
    "User(" + email + ")"
  }
}

object User {

  val user = {
      get[String]("email") ~
      get[String]("password")map {
      case email ~ password =>
        User(email, password)
    }
  }

  /**
   * Retrieve all users.
   */
  def findAll: List[User] = {
    DB.withConnection {
      implicit c =>
        SQL(
          """
            select email, password from account
          """.stripMargin
        ).as(user *)
    }
  }



  /**
   * Retrieve a User from email.
   */
  def findByEmail(email: String): Option[User] = {
    DB.withConnection {
      implicit c =>
        SQL(
          """
            select email, password from account
            where email = {email}
          """.stripMargin
        ).on(
            'email -> email
          ).using(user).singleOpt
    }
  }



  /**
   * Authenticate a User.
   */
  def authenticate(email: String, password: String): Boolean = {
    DB.withConnection {
      implicit c =>
        SQL(
          """
            select email, password from account
            where email = {email}
              and password = {password}
          """.stripMargin
        ).on(
            'email -> email,
            'password -> password
        ).using(user).singleOpt
        match {
          case Some(myUser)  => true
          case None => false
        }
    }

  }


}

