name := """nfatracker"""

version := "2.6.x"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.2"

libraryDependencies += guice
libraryDependencies += "com.typesafe.play" %% "play-slick" %  "3.0.0-M5"
libraryDependencies += "com.typesafe.play" %% "play-slick-evolutions" % "3.0.0-M5"
libraryDependencies += "com.h2database" % "h2" % "1.4.194"
//libraryDependencies += "com.github.haifengl" % "smile-netlib" % "1.4.0"
libraryDependencies += "com.github.haifengl" %% "smile-scala" % "1.4.0"
libraryDependencies += "com.univocity" % "univocity-parsers" % "2.5.2"
libraryDependencies += "com.github.nscala-time" %% "nscala-time" % "2.16.0"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"
libraryDependencies += "org.webjars" % "bootstrap" % "3.3.7"
libraryDependencies += "org.webjars" % "jquery" % "3.2.1"

libraryDependencies += specs2 % Test
  

