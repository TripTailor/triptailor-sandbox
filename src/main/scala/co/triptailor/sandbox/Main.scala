package co.triptailor.sandbox

import java.io.File

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.util.ByteString
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object Main extends Common with Setup with NLPAnalysisService {

  implicit val system = ActorSystem("triptailor-sandbox-system")
  implicit val materializer = ActorMaterializer()
  implicit val ec = ExecutionContext.global

  lazy val config = ConfigFactory.load("nlp")

  // TODO: Obtain seed from configuration
  val r = new scala.util.Random(1000)
  val nbrStreams = 3

  def main(args: Array[String]): Unit = {
    parseFileReviews(new File(config.getString("nlp.reviewsFile")))
      .via(produceRatedReviews)
      .map(review => (split(nbrStreams), review))
      .groupBy(nbrStreams, _._1)
      .mapAsync(parallelism = nbrStreams) { case (nbr, review) =>
        Source.single(review).map{ review =>
          println(review)
          ByteString(review.toString + "\n")
        }.runWith(FileIO.toFile(new File(s"$nbr"), append = true))
      }.mergeSubstreams
      .runForeach(println) onComplete {
        case Success(v) =>
          println("Done rating reviews")
          system.terminate()
        case Failure(e) =>
          println(e.getClass)
          println(e.getMessage)
          println(e.getStackTrace.mkString("\n"))
          system.terminate()
      }
  }

}
