name := "heilsusaga"
 
version := "1.0" 
      
lazy val `heilsusaga` = (project in file(".")).enablePlugins(PlayJava, PlayEbean)

scalaVersion := "2.12.2"
resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"
resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
resolvers += "Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases/"

val mysql = "mysql" % "mysql-connector-java" % "5.1.+"
val openSAML = "org.opensaml" % "opensaml" % "2.6.4"
val bcrypt = "org.mindrot" % "jbcrypt" % "0.3m"
val apache_poi = "org.apache.poi" % "poi" % "3.8"
val apache_poi_ooxml =  "org.apache.poi" % "poi-ooxml" % "3.9"

libraryDependencies ++= Seq(
  jdbc ,
  ehcache ,
  ws ,
  specs2 % Test ,
  guice,
  mysql,
  openSAML,
  bcrypt,
  apache_poi,
  apache_poi_ooxml)

unmanagedResourceDirectories in Test +=  baseDirectory ( _ /"target/web/public/test" ).value

      