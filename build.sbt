name := "jsvc-wrapper"

version := "1.0"

scalaVersion := "2.11.4"

libraryDependencies += "commons-daemon" % "commons-daemon" % "1.0.15"

libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.2"

libraryDependencies += "org.codehaus.janino" % "janino" % "2.7.7"

libraryDependencies += "net.logstash.logback" % "logstash-logback-encoder" % "3.0"

assemblyJarName in assembly := "jsvc-demo-app.jar"


