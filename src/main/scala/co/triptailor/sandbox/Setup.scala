package co.triptailor.sandbox

import java.io.File

import akka.stream.io.Framing
import akka.stream.scaladsl._
import akka.util.ByteString
import org.joda.time.LocalDate

import scala.concurrent.Future
import scala.util.Random

import scala.collection.JavaConverters._

trait Setup { self: Common with NLPAnalysisService with ClassificationService =>
  def gen: Random
  def modelSize: Int

  def parseFileReviews(f: File): Source[UnratedReview, Future[Long]] =
    FileIO.fromFile(f)
      .via(Framing.delimiter(ByteString(System.lineSeparator), maximumFrameLength = Int.MaxValue, allowTruncation = true))
      .map(_.utf8String)
      .drop(1) // Drops CSV headers
      .map(toUnratedReview)

  def splitReviewsIntoDocuments =
    Flow[RatedReview]
      .map(review => (split(modelSize), review))
      .fold(Map.empty[Int, RatedDocument]) { case (mappings, (nbr, review)) =>
        val document = mappings.getOrElse(nbr, RatedDocument(Seq(), Map()))
        mappings.updated(
          nbr,
          document.copy(reviews = document.reviews :+ review, metrics = mergeMetrics(document.metrics, review.metrics)))
      }
      .map(_.values.toSeq)

  def classifyDocuments =
    Flow[Seq[RatedDocument]]
      .map(classifyByTags)
      .map(_.zipWithIndex)
      .mapConcat(_.to[collection.immutable.Seq])
      .mapAsync(parallelism = modelSize) { case (classifiedDoc, idx) =>
        Source.single(classifiedDoc).map { doc =>
          ByteString(editDocument(doc))
        }.runWith(FileIO.toFile(new File(s"${idx + 1}")))
      }

  private def split(n: Int) = gen.nextInt(modelSize) + 1

  private def toUnratedReview(data: String) = {
    val Seq(date, text @ _*) = data.split(",").toSeq
    UnratedReview(new LocalDate(date), text.mkString(","))
  }

  private def editDocument(doc: ClassifiedDocument) = {
    val tags = config.getStringList("classification.tags").asScala
    
    val tagSentiments = doc.ratedTags.map(tag => tag.attribute + ":" + tag.rating).mkString("\n")
    
    def reviewContainsTag(r: RatedReview) =
      r.metrics.foldLeft(false)((contains, metric) => contains || tags.contains(metric._1))
    def tokenSentiments(metrics: Map[String, RatingMetrics]) =
      metrics.filter(token => tags.contains(token._1)).map{case (tag, metrics) => tag + ":" + metrics.sentiment}
    def editReview(r: RatedReview) =
      Seq(r.date.toString(), tokenSentiments(r.metrics).mkString(","), r.text).mkString(" | ")
    val reviewsText = doc.document.reviews.filter(reviewContainsTag).map(editReview).mkString("\n-----------\n")
    
    Seq(doc.rating, tagSentiments, reviewsText).mkString("\n===============\n")
  }

}
