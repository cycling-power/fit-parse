package de.jagile.fitparse

import java.io.FileInputStream

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

class MainTest extends Specification {

  private val activityFile = "20140813-222801-1-1328-ANTFS-4-0.FIT"

  step {

  }

  def baseDir: Path = Paths.get(this.getClass.getClassLoader.getResource("garmin-device").toURI)

  def listFiles(dir: Path): List[Path] = Files.list(dir).iterator().toList

  def opt(value: java.lang.Short): Option[Int] = Option(value.asInstanceOf[Int])

  def opt(value: java.lang.Integer): Option[Int] = if(value == null) None else Some(value)

  "list activities" should {
    "check base dir is an existing directory" in {
      baseDir.toFile.isDirectory === true
    }

    "list files in baseDir" in {
      listFiles(baseDir) contains baseDir.resolve(activityFile)
    }

    s"read start time from activity $activityFile" in {
      val file = baseDir.resolve(activityFile).toFile

      val result = mutable.MutableList[String]()

      val b = new MesgBroadcaster(new Decode())
      b.addListener(new ActivityMesgListener {
        override def onMesg(mesg: ActivityMesg): Unit = {
          result += s"${mesg.getTimestamp}"
        }
      })
      b.run(new FileInputStream(file))

      result.toList === List("Wed Aug 13 23:51:41 CEST 2014")
    }

    s"read average heart rate from activity $activityFile" in {
      val file = baseDir.resolve(activityFile).toFile

      val result = mutable.MutableList[Session]()

      val b = new MesgBroadcaster(new Decode())
      b.addListener(new SessionMesgListener {
        override def onMesg(mesg: SessionMesg): Unit = {
          Option(null)
          val heartRate = MinMaxAvg(opt(mesg.getMinHeartRate), opt(mesg.getAvgHeartRate), opt(mesg.getMaxHeartRate))
          result +=
            Session(
              mesg.getStartTime.getTimestamp,
              mesg.getTotalTimerTime.toLong,
              MinMaxAvg(None, opt(mesg.getAvgCadence), opt(mesg.getMaxCadence)),
              heartRate,
              MinMaxAvg(None, opt(mesg.getAvgPower), opt(mesg.getMaxPower))
            )
        }
      })
      b.run(new FileInputStream(file))

      result.toList === List(Session(776896081,5010,MinMaxAvg(None,Some(103),Some(170)),MinMaxAvg(None,Some(139),Some(176)),MinMaxAvg(None,Some(165),Some(346))))
    }
  }
}