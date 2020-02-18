package com.nikhil.akka.streams.fun

import akka.Done
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}

import scala.concurrent.Future
import scala.util.Success

object Streams extends App {
  implicit val system = ActorSystem("Streams")
  val materializer = ActorMaterializer()

  val source = Source(1 to 10)
  val sink = Sink.foreach(println)

  val graph = source.to(sink)
//  graph.run()

  val flow = Flow[Int].map(_ + 1)

//  all of the following are same
//  basically sink -> flow -> sink
//  source.via(flow).to(sink).run()
//  source.map(_ + 1).to(sink).run()
//  val mapSource = Source(1 to 10).map(_ + 1)
//  mapSource.runForeach(println)

  /**
    * Exercise : stream of names, keep the first 2 names with length > 5
    */
  val namesSource = Seq(
    "Verena",
    "Timo",
    "Charlie",
    "Leo",
    "Florian",
    "Christina",
    "Nikhil",
    "Manuel",
    "Daniel",
  )
  val longNameFlow = Flow[String].filter(_.length > 5).take(2)
  val printNameSink: Sink[String, Future[Done]] = Sink.foreach(println)
  Source(namesSource).via(longNameFlow).to(printNameSink).run()

  // same as

  Source(namesSource).filter(_.length > 5).take(2).runForeach(println)
}

object Materializers extends App {

  /**
    * Exercise :
    * 1. Return the last element out of a source
    * 2. Total word count out of a stream of sentences (use Flow.map, Flow/Sink.fold, Flow/Sink.reduce
    */
  implicit val system = ActorSystem("Mat")
  val materializer = ActorMaterializer()
  import system.dispatcher
  Source(1 to 10).toMat(Sink.last)(Keep.right).run().map(println)
  Source(1 to 10).runWith(Sink.last) // same as above
  Source(
    Seq(
      "This is the first one",
      "This is the second one, longer than the first one"
    )
  ).runFold[Int](0)((count, str) => count + str.split(" ").length)
    .map(println)
}
