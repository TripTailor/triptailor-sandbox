package co.triptailor.sandbox

import java.io.File

import akka.stream.io.Framing
import akka.stream.scaladsl._
import akka.util.ByteString
import org.joda.time.LocalDate

import scala.concurrent.Future
import scala.util.Random

trait Setup { self: Common with NLPAnalysisService with ClassificationService =>
  def gen: Random
  def modelSize: Int
  def nbrReviews: Int
  def occurrence: Double

  def parseFileReviews(f: File): Source[UnratedReview, Future[Long]] =
    FileIO.fromFile(f)
      .via(Framing.delimiter(ByteString(System.lineSeparator), maximumFrameLength = Int.MaxValue, allowTruncation = true))
      .map(_.utf8String)
      .drop(1) // Drops CSV headers
      .map(toUnratedReview)
      
  def matchModelOccurrence =
    Flow[UnratedReview]
      .fold((Seq.empty[UnratedReview], Seq.empty[UnratedReview])) {
        case ((matches, nonMatches), review) if tags.exists(review.text.contains) =>
          (matches :+ review, nonMatches)
        case ((matches, nonMatches), review) =>
          (matches, nonMatches :+ review)
      }
      .mapConcat { case (matches, nonMatches) =>
        val occurrencePartition    = gen.shuffle(matches).take((occurrence * nbrReviews).toInt)
        val notOccurrencePartition = gen.shuffle(nonMatches).take(nbrReviews - occurrencePartition.size)
        
        (occurrencePartition ++ notOccurrencePartition).to[collection.immutable.Seq]
      }

  def splitReviewsIntoDocuments =
    Flow[RatedReview]
      .map(review => (split, review))
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

  private def split = gen.nextInt(modelSize) + 1

  private def toUnratedReview(data: String) = {
    val Seq(date, text @ _*) = data.split(",").toSeq
    UnratedReview(new LocalDate(date), text.mkString(","))
  }

  private def editDocument(doc: ClassifiedDocument) = {
    val docInformation = doc.rating + "\nn: " + doc.document.metrics.foldLeft(0){ case (sum, (tag, metrics)) =>
        sum + metrics.freq.toInt
    }
    
    val tagRatings = doc.ratedTags.map(tag => {
      tag.attribute + ": " + tag.rating + " - " + doc.document.metrics(tag.attribute).freq.toInt
    }).mkString("\n")
    
    def tokenSentiments(metrics: Map[String, RatingMetrics]) =
      metrics.filter(token => tags.contains(token._1)).map{case (tag, metrics) => tag + ":" + metrics.sentiment}
    def editReview(r: RatedReview) =
      Seq(r.date.toString(), tokenSentiments(r.metrics).mkString(","), r.text).mkString(" | ")
    val reviewsText = doc.document.reviews.map(editReview).mkString("\n-----------\n")
    
    Seq(docInformation, tagRatings, reviewsText).mkString("\n===============\n")
  }

}