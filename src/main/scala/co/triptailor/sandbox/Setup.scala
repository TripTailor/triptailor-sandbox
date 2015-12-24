package co.triptailor.sandbox

import java.io.File

import org.joda.time.LocalDate

import scala.concurrent.Future

import akka.stream.SourceShape
import akka.stream.scaladsl.{ Source, FileIO }
import akka.stream.io.Framing
import akka.util.ByteString

trait Setup { self: Common =>

  def parseFileReviews(f: File): Source[UnratedReview, Future[Long]] =
    FileIO.fromFile(f)
      .via(Framing.delimiter(ByteString(System.lineSeparator), maximumFrameLength = Int.MaxValue, allowTruncation = true))
      .map(_.utf8String)
      .drop(1) // Drops CSV headers
      .map(toUnratedReview)

  private def toUnratedReview(data: String) = {
    val Seq(date, text @ _*) = data.split(",").toSeq
    UnratedReview(new LocalDate(date), text.mkString)
  }

}
