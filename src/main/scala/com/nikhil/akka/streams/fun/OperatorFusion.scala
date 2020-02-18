package com.nikhil.akka.streams.fun

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}

object OperatorFusion extends App {
  implicit val system = ActorSystem("OperatorFusion")
  val materializer = ActorMaterializer()
  val simpleSource = Source(1 to 50)
  val simpleFlow = Flow[Int].map(_ + 1)
  val simpleFlow2 = Flow[Int].map(_ * 10)
  val simpleSink = Sink.foreach(println)

  // Runs on the same actor, uses single single CPU core
  simpleSource.via(simpleFlow).via(simpleFlow2).to(simpleSink)

  // Async invokes new actors
  simpleSource
    .via(simpleFlow)
    .async // actor 1
    .via(simpleFlow2)
    .async // actor 2
    .to(simpleSink) // actor 3

  // this is (called operator fusion) great for quick computations because
  // there's no message passing involved among diff actors

  // following is called async boundary
  val f = simpleSource
    .map { a =>
      println("flow1"); a + 1
    }
    .async
    .map { a =>
      println("flow2"); a * 2
    }
    .async
    .to(Sink.last)
    .run()

}
