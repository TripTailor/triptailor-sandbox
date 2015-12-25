package co.triptailor.sandbox

import java.io.File

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object Main extends Common with Setup with NLPAnalysisService {

  implicit val system = ActorSystem("triptailor-sandbox-system")
  implicit val materializer = ActorMaterializer()
  implicit val ec = ExecutionContext.global

  lazy val config = ConfigFactory.load("nlp")

  def main(args: Array[String]): Unit = {
    parseFileReviews(new File(config.getString("nlp.reviewsFile")))
      .via(produceRatedReviews)
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
