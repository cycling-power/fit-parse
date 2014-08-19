package de.jagile.fitparse

import java.io.{InputStream, FileInputStream}

import com.garmin.fit._
import org.specs2.mutable._
import java.nio.file.{Files, Paths, Path}
import scala.collection.JavaConversions._
import scala.collection.mutable

case class MinMaxAvg[T](min: Option[T], max: Option[T], avg: Option[T])

case class Session(
  startTimestamp: Long,
  totalMovingTime: Long,
  cadence: MinMaxAvg[Int],
  heartRate: MinMaxAvg[Int],
  power: MinMaxAvg[Int]
)
case class Device(manufacturer: Int, product: Int, serialNumber: Long)
case class Activity(timestamp: Long, device: Device, sessions: List[Session])

class MainTest extends Specification {

  private val activitiesFile = "20140813-222801-1-1328-ANTFS-4-0.FIT"

  def baseDir: Path = Paths.get(this.getClass.getClassLoader.getResource("garmin-device").toURI)

  def listFiles(dir: Path): List[Path] = Files.list(dir).iterator().toList

  private[fitparse] def opt(value: java.lang.Short): Option[Int] = Option(value.asInstanceOf[Int])

  private[fitparse] def opt(value: java.lang.Integer): Option[Int] = if(value == null) None else Some(value)

  "list activities" should {
    "check base dir is an existing directory" in {
      baseDir.toFile.isDirectory === true
    }

    "list files in baseDir" in {
      listFiles(baseDir) contains baseDir.resolve(activitiesFile)
    }

    s"read start time from activity $activitiesFile" in {
      val file = baseDir.resolve(activitiesFile).toFile

      val sessionMesgs = mutable.MutableList[SessionMesg]()
      var fileIdMesgs = mutable.MutableList[FileIdMesg]()
      var activityMesgs = mutable.MutableList[ActivityMesg]()

      val b = new MesgBroadcaster(new Decode())
      //b.addListener(new MesgListener { override def onMesg(mesg: Mesg): Unit = println(s"order: ${mesg.getName}")})
      b.addListener(new ActivityMesgListener { override def onMesg(mesg: ActivityMesg): Unit = activityMesgs += mesg })
      b.addListener(new FileIdMesgListener { override def onMesg(mesg: FileIdMesg): Unit = fileIdMesgs += mesg })
      b.addListener(new SessionMesgListener { override def onMesg(mesg: SessionMesg): Unit = sessionMesgs += mesg })
      b.addListener(new DeviceSettingsMesgListener { override def onMesg(mesg: DeviceSettingsMesg): Unit = println(s"DeviceSettings: ${mesg.getUtcOffset}") })

      b.run(new FileInputStream(file))

      // construct presentation
      val sessions = sessionMesgs.map{mesg =>
        val heartRate = MinMaxAvg(opt(mesg.getMinHeartRate), opt(mesg.getAvgHeartRate), opt(mesg.getMaxHeartRate))
        val cadence: MinMaxAvg[Int] = MinMaxAvg(None, opt(mesg.getAvgCadence), opt(mesg.getMaxCadence))
        val power: MinMaxAvg[Int] = MinMaxAvg(None, opt(mesg.getAvgPower), opt(mesg.getMaxPower))
        Session(mesg.getStartTime.getTimestamp, mesg.getTotalTimerTime.toLong, cadence, heartRate, power)
      }
      //val activities = activityMesgs.map{mesg => s"${mesg.getTimestamp.getDate}"}
      val activities = activityMesgs.map{mesg => Activity(
        mesg.getTimestamp.getDate.getTime,
        fileIdMesgs.take(1).map{mesg =>  Device(mesg.getManufacturer, mesg.getProduct, mesg.getSerialNumber)}.head,
        sessions.toList
      )}.toList

      activities === List(Activity("1407966701000".toLong,
        Device(1,1328,"3845061855".toLong),
        List(Session(776896081,5010,MinMaxAvg(None,Some(103),Some(170)),MinMaxAvg(None,Some(139),Some(176)),MinMaxAvg(None,Some(165),Some(346))))
      ))
    }

  }
}