package models

import play.api.db._
import play.api.Play.current
import anorm._
import anorm.SqlParser._
import org.mindrot.jbcrypt.BCrypt


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
   * Create a new user account
   */
  def create(email: String, password: String): Option[Long] = {
    DB.withConnection {
      implicit c =>
        SQL(
          """
            insert into account
            (email, password)
            values({email},{password})
          """.stripMargin
        ).on(
            'email -> email,
            'password -> BCrypt.hashpw(password, BCrypt.gensalt())
          ).executeInsert()
    }
  }

  /**
   * Delete a user account
   */
  def delete(email: String) {
    DB.withConnection {
      implicit c =>
        SQL(
          """
            delete from account
            where email = {email}
          """.stripMargin
        ).on(
            'email -> email
          ).executeUpdate()
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
          """.stripMargin
        ).on(
            'email -> email
          ).using(user).singleOpt
        match {
          case Some(myUser) =>
            BCrypt.checkpw(password, myUser.password)
          case _ => false
        }
    }
  }


}

