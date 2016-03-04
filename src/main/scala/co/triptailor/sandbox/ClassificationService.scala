package co.triptailor.sandbox

import scala.collection.JavaConverters._

/**
  * csen = (cfreq * sen) / freq
  *
  * BM25 Model
  * rating = csen / (1-b + b(dl/avdl))
  *
  * b      - classification normalizer
  * rating - computed rating for a document based on tags that match query
  * csen   - computed sentiment for a document
  * dl     - document length (number of tags that match query)
  * avdl   - average document length (average number of tags for a given model that match query)
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
  def classifyByTags(m: Model): Seq[ClassifiedDocument] = {
    val avdl = compute_avdl(m, tags)

    (for {
      doc  ← m
      dl   = compute_dl(doc)
      ratedTags = rateTags(doc.metrics, tags, dl, avdl)
    } yield ClassifiedDocument(doc, rating = compute_bm25(ratedTags, tags, dl, avdl), ratedTags)).sorted
  }

  private def compute_bm25(ratedTags: Seq[RatedTag], tags: Set[String], dl: Double, avdl: Double): Double =
    ratedTags.foldLeft( 0d ) { (rating, token) =>
      rating + token.rating
    }
  
  private def rateTags(ratingMetrics: Map[String, RatingMetrics], tags: Set[String], dl: Double, avdl: Double) = {
    val filteredMetrics = getMetricsOfTags(tags, ratingMetrics)
    filteredMetrics.map { case (tag, metrics) =>
      RatedTag(tag, (metrics.cfreq * metrics.sentiment) / (metrics.freq * (1 - b + b * (dl / avdl))))
    }.toSeq
  }

  private def compute_avdl(m: Model, tags: Set[String]): Double =
    m.foldLeft( 0d )(_ + compute_dl(_)) / m.size

  private def compute_dl(doc: RatedDocument): Double =
    doc.metrics.map(_._2.freq).sum
    
  private def getMetricsOfTags(tags: Set[String], metrics: Map[String, RatingMetrics]) =
    metrics.filter(token => tags.contains(token._1))
}

object ClassificationService {
  private implicit val classifiedDocumentAscending: Ordering[ClassifiedDocument] =
    Ordering.by[ClassifiedDocument, Double](-_.rating)
}
