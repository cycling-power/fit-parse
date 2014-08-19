organization  := "jagile"

version       := "0.1"

scalaVersion  := "2.11.2"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

resolvers += Resolver.mavenLocal

libraryDependencies ++= {
  Seq(
    "com.garmin.fit"      %  "fit"  % "1.0.12.0",
    "org.specs2"          %%  "specs2"        % "2.4" % "test"
  )
}

