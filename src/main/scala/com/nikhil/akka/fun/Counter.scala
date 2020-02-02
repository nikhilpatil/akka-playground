package com.nikhil.akka.fun

import akka.actor.{Actor, ActorSystem, Props}

class Counter extends Actor {
  import Counter._

  override def receive: Receive = count(0)

  def count(counter: Int): Receive = {
    case Increment => context.become(count(counter + 1))
    case Decrement => context.become(count(counter - 1))
    case Print     => println("Count is : " + counter)
  }
}

object Counter {
  case object Increment
  case object Decrement
  case object Print
}

object Main extends App {
  import com.nikhil.akka.fun.Counter._
  val system = ActorSystem("CounterSystem")
  val counter = system.actorOf(Props[Counter])
  counter ! Increment
  counter ! Increment
  counter ! Decrement
  counter ! Decrement
  counter ! Increment
  counter ! Print
  counter ! Decrement
  counter ! Increment
  counter ! Increment
  counter ! Print
}
