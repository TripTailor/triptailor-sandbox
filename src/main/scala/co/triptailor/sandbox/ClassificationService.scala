package co.triptailor.sandbox

import scala.collection.JavaConverters._

/**
  * csen = (cfreq * sen) / freq
  *
  * BM25 Model
  * rating = csen / (1-b + b(dl/avdl))
  *
  * b      - classification normalizer
  * rating - computed rating for a document using bm25 (https://en.wikipedia.org/wiki/Okapi_BM25)
  * csen   - computed sentiment for a document
  * dl     - document length (total number of attributes contained in document)
  * avdl   - average document length (average number of attributes contained in a document)
  */
trait ClassificationService { self: Common =>
  import ClassificationService._

  type Model = Seq[RatedDocument]

  val b = config.getDouble("classification.classificationNormalizer")
  /** Used to draw comparison with model **/
  val tags = config.getStringList("classification.tags").asScala.toSet

  /**
    * @param m collection of documents
    * @return ordered collection of `ClassifiedDocument`
    */
  def classifyByTags(m: Model): Seq[ClassifiedDocument] =
    (for {
      doc       ← m
      dl        = compute_dl(doc)
      ratedTags = rateTags(doc.metrics, tags, dl, compute_avdl(m, tags))
    } yield ClassifiedDocument(doc, rating = compute_bm25(ratedTags), ratedTags)).sorted

  private def compute_bm25(ratedTags: Seq[RatedTag]) =
    ratedTags.foldLeft( 0d )(_ + _.rating)

  private def rateTags(ratingMetrics: Map[String, RatingMetrics], tags: Set[String], dl: Double, avdl: Double) =
    ratingMetrics.collect {
      case (tag, metrics) if tags contains tag =>
        RatedTag(tag, (metrics.cfreq * metrics.sentiment / metrics.freq) / (1 - b + b * (dl / avdl)))
    }.toSeq

  private def compute_avdl(m: Model, tags: Set[String]): Double =
    m.foldLeft( 0d )(_ + compute_dl(_)) / m.size

  private def compute_dl(doc: RatedDocument): Double =
    doc.metrics.map(_._2.freq).sum
}

object ClassificationService {
  private implicit val classifiedDocumentAscending: Ordering[ClassifiedDocument] =
    Ordering.by[ClassifiedDocument, Double](-_.rating)
}