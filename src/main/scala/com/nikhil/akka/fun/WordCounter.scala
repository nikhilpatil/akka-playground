package com.nikhil.akka.fun

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.event.Logging

object WordCounterMaster {
  case class Initialize(nChildren: Int)
  case class WordCountTaskForChild(text: String, originalSender: ActorRef)
  case class WordCountReply(wordCount: Int, originalSender: ActorRef)
}

import WordCounterMaster._
class WordCounterMaster extends Actor {
  val log = Logging(context.system, "WCA")
  override def receive: Receive = {
    case Initialize(nChildren) =>
      val children: Seq[ActorRef] =
        (1 to nChildren).map(
          index => context.actorOf(Props[WordCounterChild], s"ChildActor$index")
        )
      context.become(withChildren(children))
  }
  def withChildren(children: Seq[ActorRef]): Receive = {
    case text: String =>
      val head = children.head
      println(s"Assigning task to ${head.path.name}")
      head ! WordCountTaskForChild(text, sender)
      context.become(withChildren(children.tail :+ head))

    case WordCountReply(count, originalSender) =>
      println(s"${sender.path.name} replied")
      originalSender ! count
  }
}

class WordCounterChild extends Actor {
  override def receive: Receive = {
    case WordCountTaskForChild(text, originalSender) =>
      val wCount = text.trim.split(" ").length
      sender ! WordCountReply(wCount, originalSender)
  }
}

object WordCounter extends App {
  val system = ActorSystem("WordCounterSystem")

  class TestActor extends Actor {
    override def receive: Receive = {
      case "Go" =>
        val master =
          system.actorOf(Props[WordCounterMaster], "WordCounterMaster")
        master ! Initialize(3)
        master ! "This is a six word sentence"
        master ! "This is a six word sentence bar ka"
        master ! "This is a six word sentence bar bar la"
        master ! "This is a six word sentence lal la la la l   "
      case count: Int => println(count)
    }
  }

  private val testActor: ActorRef =
    system.actorOf(Props[TestActor])
  testActor ! "Go"

}
