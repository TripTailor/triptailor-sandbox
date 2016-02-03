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
  val tags = config.getStringList("classification.tags").asScala

  /**
    * @param m collection of documents
    * @return ordered collection of `ClassifiedDocument`
    */
  def classifyByTags(m: Model): Seq[ClassifiedDocument] = {
    val avdl = compute_avdl(m, tags)

    (for {
      doc  ← m
      csen = compute_csen(doc, tags)
      dl   = compute_dl(doc)
    } yield ClassifiedDocument(doc, rating = compute_bm25(csen, dl, avdl))).sorted
  }

  private def compute_bm25(csen: Double, dl: Double, avdl: Double): Double =
    csen / (1 - b + b * (dl / avdl))

  private def compute_csen(doc: RatedDocument, tags: Seq[String]): Double =
    tags.flatMap(doc.metrics.get).foldLeft( 0d ) { (csens, metrics) =>
      csens + (metrics.cfreq * metrics.sentiment) / metrics.freq
    }

  private def compute_avdl(m: Model, tags: Seq[String]): Double =
    m.foldLeft( 0d )(_ + compute_dl(_)) / m.size

  private def compute_dl(doc: RatedDocument): Double =
    (for {
      review   ← doc.reviews
      sentence ← review.sentences
    } yield sentence.metrics.keys.size).sum

}

object ClassificationService {
  private implicit val classifiedDocumentAscending: Ordering[ClassifiedDocument] =
    Ordering.by[ClassifiedDocument, Double](-_.rating)
}
