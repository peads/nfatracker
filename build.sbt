name := """nfatracker"""

version := "0.4.1"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.3"

libraryDependencies += guice
libraryDependencies += "com.typesafe.play" %% "play-slick" %  "3.0.0-M5"
libraryDependencies += "com.typesafe.play" %% "play-slick-evolutions" % "3.0.0-M5"
//libraryDependencies += "com.github.haifengl" % "smile-netlib" % "1.4.0"
libraryDependencies += "com.github.haifengl" %% "smile-scala" % "1.4.0"
libraryDependencies += "com.univocity" % "univocity-parsers" % "2.5.2"
libraryDependencies += "com.github.nscala-time" %% "nscala-time" % "2.16.0"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"
libraryDependencies += "org.webjars" % "bootstrap" % "3.3.7"
libraryDependencies += "org.webjars" % "jquery" % "3.2.1"
libraryDependencies += "org.webjars" % "flot" % "0.8.3-1"
libraryDependencies += "org.webjars" % "bootstrap-datepicker" % "1.7.1"
libraryDependencies += "org.webjars.bower" % "spin.js" % "2.3.2"
libraryDependencies += "org.postgresql" % "postgresql" % "9.4.1212"

libraryDependencies += specs2 % Test
  

