package co.triptailor.sandbox

import org.joda.time.LocalDate

case class UnratedReview(date: LocalDate, text: String)

object Sentiment {
  val VeryPositive = 3
  val Positive     = 2
  val Neutral      = 1
  val Negative     = -1
  val VeryNegative = -2

  def apply(value: Int) =
    value match {
      case 0 => VeryNegative
      case 1 => Negative
      case 2 => Neutral
      case 3 => Positive
      case 4 => VeryPositive
    }
}

case class ReviewMetaData(reviewer: Option[String], city: Option[String], gender: Option[String], age: Option[Int])
case class UnratedDocument(reviewData: Seq[UnratedReview])

case class RatingMetrics(sentiment: Int, freq: Double, cfreq: Double)
case class Position(start: Int, end: Int) {
  override def productPrefix = ""
}

case class AnnotatedToken(attribute: String, position: Position)
case class AnnotatedPositionedToken(attribute: String, positions: Seq[Position])
case class AnnotatedSentence(text: String, tokens: Seq[AnnotatedPositionedToken], sentiment: Int)

case class RatedSentence(positionedSentence: AnnotatedSentence, metrics: Map[String, RatingMetrics])
case class RatedReview(text: String, tokens: Seq[AnnotatedPositionedToken], sentences: Seq[RatedSentence],
                       metrics: Map[String, RatingMetrics], date: LocalDate)
case class RatedDocument(reviews: Seq[RatedReview], metrics: Map[String, RatingMetrics])

case class ClassifiedDocument(document: RatedDocument, rating: Double)