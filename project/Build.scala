import sbt._
import Keys._
import play.Project

object ApplicationBuild extends Build {

  val appName         = "sendR"
  val appVersion      = "1.2-beta1"

  val appDependencies = Seq(
    "org.webjars" %% "webjars-play" % "2.2.1-2" exclude("org.webjars", "jquery"),
    "org.webjars" % "jquery" % "1.10.1",                                   // AngularJS can't handle jQuery 2.x
    "org.webjars" % "bootstrap" % "3.0.3" exclude("org.webjars", "jquery"),
    "org.webjars" % "angularjs" % "1.2.9" exclude("org.webjars", "jquery"),
    "org.webjars" % "requirejs-domready" % "2.0.1" exclude("org.webjars", "jquery"),
    "org.webjars" % "requirejs" % "2.1.10" exclude("org.webjars", "jquery"),
    "net.sf.opencsv" % "opencsv" % "2.3"
  )

  val main = Project(appName, appVersion, appDependencies).settings(
    resolvers += Resolver.url("sbt-plugin-releases", new URL("http://repo.scala-sbt.org/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns)
  )


}
