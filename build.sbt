import java.io.FileInputStream
import java.nio.file.{StandardCopyOption, CopyOption, Files}
import javax.crypto.spec.{IvParameterSpec, SecretKeySpec}
import javax.crypto.{CipherInputStream, Cipher}

organization  := "jagile"

version       := "0.1"

scalaVersion  := "2.11.2"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

resolvers += Resolver.mavenLocal

libraryDependencies ++= {
  Seq(
    "com.garmin.fit"      %  "fit"  % "1.0.12.0",
    "org.specs2"          %%  "specs2"        % "2.4" % "test"
  ).filterNot(m => System.getenv("FIT_TOKEN") != null && m.name == "fit")
}

// decode FIT library
managedClasspath in Test <<= (managedClasspath in Test, baseDirectory, target) map {
  (cp, bd, t) => {
    val token = System.getenv("FIT_TOKEN")
    if (token != null) {
      val cipherType = "AES/CFB8/PKCS5Padding"
      val parameterSpec = new IvParameterSpec(List.fill(16)("0".toByte).toArray)
      // encode file to library (TEMP)
      if((bd / "libs/fit-1.0.12.0.jar").exists()) {
        val cipherEncode = Cipher.getInstance(cipherType)
        cipherEncode.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(token.getBytes("UTF-8"), "AES"), parameterSpec)
        val encoding = new CipherInputStream(new FileInputStream(bd / "libs/fit-1.0.12.0.jar"), cipherEncode)
        Files.copy(encoding, (bd / "libs/fit-1.0.12.0.jar.enc").toPath, StandardCopyOption.REPLACE_EXISTING)
      }
      println("Decoding fit-1.0.12.0.jar.enc....")
      // decode file into target
      val cipherDecode = Cipher.getInstance(cipherType)
      cipherDecode.init(Cipher.DECRYPT_MODE, new SecretKeySpec(token.getBytes("UTF-8"), "AES"), parameterSpec)
      val decoding = new CipherInputStream(new FileInputStream(bd / "libs/fit-1.0.12.0.jar.enc"), cipherDecode)
      Files.copy(decoding, (t / "fit-1.0.12.0.jar").toPath, StandardCopyOption.REPLACE_EXISTING)
      cp :+ Attributed.blank(t / "fit-1.0.12.0.jar")
    } else {
      cp
    }
}}
