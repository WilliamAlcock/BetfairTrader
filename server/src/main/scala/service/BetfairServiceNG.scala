package service

import akka.actor.ActorSystem
import server.Configuration

import scala.concurrent._
import scala.language.postfixOps

class BetfairServiceNG(val config: Configuration, val command: BetfairServiceNGCommand)
                      (implicit executionContext: ExecutionContext, system: ActorSystem) extends BetfairService