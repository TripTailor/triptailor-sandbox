package co.triptailor.sandbox

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

import com.typesafe.config.Config

trait Common {
  implicit def system: ActorSystem
  implicit def materializer: ActorMaterializer
  def config: Config
}
