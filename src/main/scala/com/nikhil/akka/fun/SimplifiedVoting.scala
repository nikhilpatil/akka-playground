package com.nikhil.akka.fun

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import com.nikhil.akka.fun.Citizen.{VoteStatus, VoteStatusReply}

object Citizen {
  case class Vote(candidate: String)
  case object VoteStatus
  case class VoteStatusReply(maybeCandidate: Option[String])
}

class Citizen extends Actor {
  import Citizen._

  override def receive: Receive = readyToVote

  def readyToVote: Receive = {
    case Vote(candidate) => context.become(votedFor(candidate))
    case VoteStatus      => sender ! VoteStatusReply(None)
  }

  def votedFor(candidate: String): Receive = {
    case Vote(candidate) => context.become(votedFor(candidate))
    case VoteStatus      => sender ! VoteStatusReply(Some(candidate))
  }
}

object VoteAggregator {
  case class Aggregate(citizens: Set[ActorRef])
  case object Print
}

class VoteAggregator extends Actor with ActorLogging {
  import VoteAggregator._

  override def receive: Receive = readyToAgrregate

  def readyToAgrregate: Receive = {
    case Aggregate(citizens) =>
      citizens.foreach(_ ! VoteStatus)
      context.become(aggregating(citizens))
  }

  def aggregating(remaining: Set[ActorRef],
                  votes: Map[String, Int] = Map.empty): Receive = {
    case VoteStatusReply(Some(candidate)) if remaining.contains(sender) =>
      val newRemaining = remaining - sender
      val votesForCandidate = votes.getOrElse(candidate, 0) + 1
      context.become(
        aggregating(newRemaining, votes + (candidate -> votesForCandidate))
      )
      if (newRemaining.isEmpty) self ! Print
    case VoteStatusReply(None) =>
      sender ! VoteStatus //DANGER: can cause infinite loop!
    case Print =>
      println("current state of affairs: " + votes.mkString(","))
  }
}

object VotingMain extends App {
  val system = ActorSystem("VotingSystem")
  val alice = system.actorOf(Props[Citizen], "Alice")
  val bob = system.actorOf(Props[Citizen], "Bob")
  val charlie = system.actorOf(Props[Citizen], "Charlie")
  val daniel = system.actorOf(Props[Citizen], "Daniel")

  val aggregator = system.actorOf(Props[VoteAggregator])

  import Citizen._
  import VoteAggregator._

  alice ! Vote("Obama")
  bob ! Vote("Trump")
  charlie ! Vote("Bernie")
  daniel ! Vote("Bernie")

  aggregator ! Aggregate(Set(alice, bob, charlie, daniel))
}
