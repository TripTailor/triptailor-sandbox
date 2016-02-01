package co.triptailor.sandbox

import java.io.File

import akka.stream.io.Framing
import akka.stream.scaladsl._
import akka.util.ByteString
import org.joda.time.LocalDate

import scala.concurrent.Future
import scala.util.Random

trait Setup { self: Common with ClassificationService =>
  def gen: Random
  def nbrStreams: Int

  def parseFileReviews(f: File): Source[UnratedReview, Future[Long]] =
    FileIO.fromFile(f)
      .via(Framing.delimiter(ByteString(System.lineSeparator), maximumFrameLength = Int.MaxValue, allowTruncation = true))
      .map(_.utf8String)
      .drop(1) // Drops CSV headers
      .map(toUnratedReview)

  def splitReviewsIntoDocuments =
    Flow[RatedReview]
      .map(review => (split(nbrStreams), review))
      .fold(Map.empty[Int, RatedDocument]) { case (mappings, (nbr, review)) =>
        // TODO: Merge rating metrics
        val document = mappings.getOrElse(nbr, RatedDocument(Seq(), Map()))
        mappings.updated(nbr, document.copy(reviews = document.reviews :+ review, metrics = review.metrics))
      }
      .map(_.values.toSeq)

  def classifyDocuments =
    Flow[Seq[RatedDocument]]
      .map(classifyByTags)
      .map(_.zipWithIndex)
      .mapConcat(_.to[collection.immutable.Seq])
      .mapAsync(parallelism = nbrStreams) { case (classifiedDoc, idx) =>
        Source.single(classifiedDoc).map { doc =>
          ByteString(editDocument(doc))
        }.runWith(FileIO.toFile(new File(s"${idx + 1}")))
      }

  private def split(n: Int) = gen.nextInt(nbrStreams) + 1

  private def toUnratedReview(data: String) = {
    val Seq(date, text @ _*) = data.split(",").toSeq
    UnratedReview(new LocalDate(date), text.mkString(","))
  }

  private def editDocument(doc: ClassifiedDocument) = {
    def sentimentAvg(ss: Seq[Int]) = ss.sum / (ss.size * 1.0)
    def editReview(r: RatedReview) = Seq(r.date.toString(), r.sentiments.mkString(", "), sentimentAvg(r.sentiments), r.text).mkString(" | ")
    val reviewsText = doc.document.reviews.map(editReview).mkString("\n-----------\n")
    Seq(doc.rating, reviewsText).mkString("\n===============\n")
  }

}
