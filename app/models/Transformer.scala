package models

import play.api.db._
import play.api.Play.current
import anorm._
import anorm.SqlParser._
import play.Logger

import anorm.~
import scala.Some

case class Transformer(
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
  timeStampString: String,
  version: Int)



/**
 * This entity contains the Transformer configuration
 */
object Transformer {


  private val transformer: RowParser[Transformer] = {
    get[Pk[Long]]("id") ~
      get[String]("name") ~
      get[Option[String]]("category") ~
      get[Option[String]]("import_path") ~
      get[Option[String]]("import_file_extension") ~
      get[Option[String]]("import_filecontent_type") ~
      get[Option[String]]("webservice_char_set") ~
      get[Option[String]]("webservice_url") ~
      get[Option[String]]("webservice_user") ~
      get[Option[String]]("webservice_password") ~
      get[Option[Int]]("webservice_timeout") ~
      get[Option[String]]("webservice_template") ~
      get[Option[String]]("time_stamp_string") ~
      get[Int]("version") map {
      case id ~
            name ~
            category ~
            importPath ~
            importFileExtension ~
            importFilecontentType ~
            webserviceCharSet ~
            webserviceURL ~
            webserviceUser ~
            webservicePassword ~
            webserviceTimeout ~
            webserviceTemplate ~
            timeStampString ~
            version =>
        Transformer(id.get,
            name,
            category.getOrElse(null),
            importPath.getOrElse(null),
            importFileExtension.getOrElse(".csv"),
            importFilecontentType.getOrElse("cp1252"),
            webserviceCharSet.getOrElse("UTF-8"),
            webserviceURL.getOrElse(null),
            webserviceUser.getOrElse(null),
            webservicePassword.getOrElse(null),
            webserviceTimeout.getOrElse(10000),
            webserviceTemplate.getOrElse("<soap></soap>"),
            timeStampString.getOrElse("2014-01-01T00:00:00Z"),
            version)
    }
  }

  def findById(id: Long): Option[Transformer] = DB.withConnection {
    implicit c =>
      SQL(
        "select * from transformer where id = {id}"
      ).on(
          'id -> id
        ).using(transformer).singleOpt()
  }



  def create(transformer: Transformer): Option[Long] = {
    Logger.debug(s"Transformer.create")
    DB.withConnection {
      implicit c =>
        SQL( """
            insert into transformer (
               |            name,
               |            category,
               |            import_path,
               |            import_file_extension,
               |            import_filecontent_type,
               |            webservice_char_set,
               |            webservice_URL,
               |            webservice_user,
               |            webservice_password,
               |            webservice_timeout,
               |            webservice_template,
               |            time_stamp_string,
               |            version
               |                  )
                 values(
               |            {name},
               |            {category},
               |            {importPath},
               |            {importFileExtension},
               |            {importFilecontentType},
               |            {webserviceCharSet},
               |            {webserviceURL},
               |            {webserviceUser},
               |            {webservicePassword},
               |            {webserviceTimeout},
               |            {webserviceTemplate},
               |            {timeStampString},
               |            {version}
               |                 )
             """.stripMargin
        ).on(
            'name -> transformer.name,
            'category -> transformer.category,
            'importPath -> transformer.importPath,
            'importFileExtension -> transformer.importFileExtension,
            'importFilecontentType -> transformer.importFilecontentType,
            'webserviceCharSet -> transformer.webserviceCharSet,
            'webserviceURL -> transformer.webserviceURL,
            'webserviceUser -> transformer.webserviceUser,
            'webservicePassword -> transformer.webservicePassword,
            'webserviceTimeout -> transformer.webserviceTimeout,
            'webserviceTemplate -> transformer.webserviceTemplate,
            'timeStampString -> transformer.timeStampString,
            'version  -> transformer.version
          ).executeInsert()
    }
  }

  def update(transformer: Transformer): Long = {
    Logger.debug(s"Transformer.update")
    DB.withConnection {
      implicit c =>
        SQL( """
            update transformer set
            name={name},
            category={category},
            import_path={importPath},
            import_file_extension={importFileExtension},
            import_filecontent_type={importFilecontentType},
            webservice_char_set={webserviceCharSet},
            webservice_url={webserviceURL},
            webservice_user={webserviceUser},
            webservice_password={webservicePassword},
            webservice_timeout={webserviceTimeout},
            webservice_template={webserviceTemplate},
            time_stamp_string={timeStampString},
            version={version}
             where id = {id}
             """.stripMargin
        ).on(
            'id -> transformer.id,
            'name -> transformer.name,
            'category -> transformer.category,
            'importPath -> transformer.importPath,
            'importFileExtension -> transformer.importFileExtension,
            'importFilecontentType -> transformer.importFilecontentType,
            'webserviceCharSet -> transformer.webserviceCharSet,
            'webserviceURL -> transformer.webserviceURL,
            'webserviceUser -> transformer.webserviceUser,
            'webservicePassword -> transformer.webservicePassword,
            'webserviceTimeout -> transformer.webserviceTimeout,
            'webserviceTemplate -> transformer.webserviceTemplate,
            'timeStampString -> transformer.timeStampString,
            'version  -> transformer.version
          ).executeUpdate()
    }
    0L
  }

  def rename(id: Long, name: String): Long = {
    Logger.debug(s"Transformer.rename")
    DB.withConnection {
      implicit c =>
        SQL( """
            update transformer set
            name={name}
             where id = {id}
             """.stripMargin
        ).on(
            'id -> id,
            'name -> name
          ).executeUpdate()
    }
    0L
  }

  def all: List[Transformer] = {
    DB.withConnection {
      implicit c =>
        SQL(
          "select * from transformer order by category ASC, name ASC"
        ).as(transformer *)
    }
  }


//  def allIds: java.util.List[Long] = {
//    val list: java.util.List[Transformer] = find.where("1=1").orderBy("name ASC").findList
//    val ids: java.util.List[Long] = new java.util.ArrayList[Long]
//    import scala.collection.JavaConversions._
//    for (tr <- list) {
//      ids.add(tr.id)
//    }
//    ids
//  }
//
//


  def cloneTransformer(sourceId: Long): Option[Transformer] = {
    Transformer.findById(sourceId) match {
      case Some(sourceTransformer) =>
        create(sourceTransformer) match {
          case Some(cloneId) =>
            Transformer.findById(cloneId) match {
              case Some(clone) =>
                  rename(clone.id, clone.name + "_CLONE_" + clone.id)
                  Transformer.findById(clone.id)
              case None =>
                Logger.error("cloneTransformer: clone not found")
                None
            }
          case None =>
            Logger.error("cloneTransformer: cloning failed")
            None
        }
      case None =>
        Logger.error("cloneTransformer: Source not found")
        None
    }
  }



  def delete(id: Long) {
    DB.withConnection {
      implicit c =>
        SQL(
          """
            delete from transformer
            where id = {id}
          """.stripMargin
        ).on(
            'id -> id
          ).executeUpdate()
    }
  }

}




