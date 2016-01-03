package co.triptailor.sandbox

/**
  * csen = (cfreq * sen) / freq
  *
  * BM25 Model
  * rating = csen / (0.25 + 0.75(dl/avdl))
  *
  * rating - computed rating for a document based on tags that match query
  * csen   - computed sentiment for a document
  * dl     - document length (number of tags that match query)
  * avdl   - average document length (average number of tags for a given model that match query)
  */
trait ClassificationService { self: Common =>
  import ClassificationService._

  type Model = Seq[RatedDocument]

  val classificationNormalizer = config.getDouble("classification.classificationNormalizer")

  /**
    * doc: Individual document taken from model
    * shared_tar: Shared tags attributes with ratings with respect to m
    * unique_mar: Unique model attributes with ratings with respect to m
    *
    * @param m collection of documents used to compare with H
    * @param tags tags used to draw comparison with m
    * @return ordered collection of ClassifiedDocument
    */
  def classifyByTags(m: Model, tags: Seq[String]): Seq[ClassifiedDocument] = {
    val avdl = compute_avdl(m, tags)

    (for {
      doc  ← m
      csen = compute_csen(doc, tags)
      dl   = compute_dl(doc, tags)
    } yield ClassifiedDocument(doc, rating = compute_bm25(csen, dl, avdl))).sorted
  }

  private def compute_bm25(csen: Double, dl: Double, avdl: Double): Double =
    csen / (classificationNormalizer + 0.75 * (dl / avdl))

  private def compute_csen(doc: RatedDocument, tags: Seq[String]): Double =
    tags.flatMap(doc.metrics.get).foldLeft( 0d ) { (csens, metrics) =>
      csens + (metrics.cfreq * metrics.sentiment) / metrics.freq
    }

  private def compute_avdl(m: Model, tags: Seq[String]): Double =
    tags.size / m.foldLeft( 0d )(_ + compute_dl(_, tags))

  private def compute_dl(doc: RatedDocument, tags: Seq[String]): Double = {
    def metricsContainsTag(tags: Seq[String])(tuple: (String, _)) = tags.contains(tuple._1)

    (for {
      review   ← doc.reviews
      sentence ← review.sentences
    } yield sentence.metrics.count(metricsContainsTag(tags))).foldLeft( 0d )(_ + _)
  }

}

object ClassificationService {
  private implicit val classifiedDocumentAscending: Ordering[ClassifiedDocument] =
    Ordering.by[ClassifiedDocument, Double](_.rating)
}