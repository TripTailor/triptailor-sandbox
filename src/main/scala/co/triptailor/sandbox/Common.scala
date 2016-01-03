package co.triptailor.sandbox

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

import com.typesafe.config.Config

import scala.concurrent.ExecutionContext

trait Common {
  implicit def system: ActorSystem
  implicit def materializer: ActorMaterializer
  implicit def ec: ExecutionContext
  def config: Config
}
