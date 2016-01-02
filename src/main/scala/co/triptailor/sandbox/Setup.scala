package co.triptailor.sandbox

import java.io.File

import akka.stream.io.Framing
import akka.stream.scaladsl._
import akka.util.ByteString
import org.joda.time.LocalDate

import scala.util.Random

import scala.concurrent.Future

trait Setup { self: Common =>
  def gen: Random
  def nbrStreams: Int

  def parseFileReviews(f: File): Source[UnratedReview, Future[Long]] =
    FileIO.fromFile(f)
      .via(Framing.delimiter(ByteString(System.lineSeparator), maximumFrameLength = Int.MaxValue, allowTruncation = true))
      .map(_.utf8String)
      .drop(1) // Drops CSV headers
      .map(toUnratedReview)

  def splitReviewsToFiles =
    Flow[RatedReview]
      .map(review => (split(nbrStreams), review))
      .groupBy(nbrStreams, _._1)
      .mapAsync(parallelism = nbrStreams) { case (nbr, review) =>
        Source.single(review).map{ review =>
          println(review)
          ByteString(review.toString + "\n")
        }.runWith(FileIO.toFile(new File(s"$nbr"), append = true))
      }.mergeSubstreams

  private def split(n: Int) = gen.nextInt(nbrStreams) + 1

  private def toUnratedReview(data: String) = {
    val Seq(date, text @ _*) = data.split(",").toSeq
    UnratedReview(new LocalDate(date), text.mkString(","))
  }

}
