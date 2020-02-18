package com.nikhil.akka.streams.fun

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape, FlowShape}
import akka.stream.scaladsl.{Broadcast, GraphDSL, Sink, Source}
import akka.stream.scaladsl._
import org.reactivestreams.{Publisher, Subscriber}

import scala.concurrent.Future
import scala.util.Random

object Graph extends App {

  /**
    * Exercise : Feed one source into two sinks at the same time
    */
  implicit val system = ActorSystem("GraphSystem")
  val materializer = ActorMaterializer()
  val input: Source[Int, NotUsed] = Source(1 to 100)
  def printlnFun(x: Any) = println(s", ☠️ $x")
  val output1: Sink[Int, Future[Done]] = Sink.foreach(print)
  val output2: Sink[Int, Future[Done]] = Sink.foreach(printlnFun)

  RunnableGraph
    .fromGraph(GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._
      val broadcast = builder.add(Broadcast[Int](2))

//      input ~> broadcast
//      broadcast.out(0) ~> output1
//      broadcast.out(1) ~> output2

//      better way to write above is :

      input ~> broadcast
      broadcast ~> output1
      broadcast ~> output2

      ClosedShape
    })
    .run()
}

object GraphWithMergeAndBalance extends App {

  /**
    * Exercise: Build this graph
    *
    *  fast source ~>   |        |        |         |   ~>  sink 1
    *                   | merge  |    ~>  | balance |
    *  slow source ~>   |        |        |         |   ~>  sink 2
    *
    */
  implicit val system = ActorSystem("GraphSystem")
  val materializer = ActorMaterializer()

  val input: Source[Int, NotUsed] = Source(1 to 100)

  val output1: Sink[Int, Future[Int]] =
//    Sink.foreach(x => println(s"First sink $x"))
    Sink.fold[Int, Int](0)((count, _) => {
      println(s"Sink 1 count: $count"); count + 1
    })
  val output2: Sink[Int, Future[Int]] =
//    Sink.foreach(x => println(s"Second sink $x"))
    Sink.fold[Int, Int](0)({ (count, _) =>
      println(s"Sink 2 count: $count"); count + 1
    })

  RunnableGraph
    .fromGraph(GraphDSL.create() { implicit builder =>
      val merge = builder.add(Merge[Int](2))
      val balance = builder.add(Balance[Int](2))
      import GraphDSL.Implicits._
      import scala.concurrent.duration._

      input.throttle(5, 1.second) ~> merge
      input.throttle(2, 1.second) ~> merge
      merge ~> balance
      balance ~> output1
      balance ~> output2

      ClosedShape
    })
    .run()

}

object ComplexGraphsWithFlowMerge extends App {
  implicit val system = ActorSystem("GraphSystem")
  val materializer = ActorMaterializer()

  val flowShape = GraphDSL.create() { implicit builder =>
    val flowAdd = Flow.fromFunction[Int, Int](a => a + 1)
    val flowMultiply = Flow.fromFunction[Int, Int](a => a * 10)

    val broadcast = builder.add(Broadcast[Int](2))
    val merge = builder.add(Merge[Int](2))

    import GraphDSL.Implicits._

    broadcast ~> flowAdd ~> merge
    broadcast ~> flowMultiply ~> merge

    FlowShape(broadcast.in, merge.out)
  }

  Source(1 to 10).via(flowShape).to(Sink.foreach(println)).run()

}
object ComplexGraphsWithFlowCompose extends App {
  implicit val system = ActorSystem("GraphSystem")
  val materializer = ActorMaterializer()

  val flowShape = GraphDSL.create() { implicit builder =>
    val flowAdd = Flow.fromFunction[Int, Int](a => a + 1)
    val flowMultiply = Flow.fromFunction[Int, Int](a => a * 10)

    import GraphDSL.Implicits._

    val flowAddShape = builder.add(flowAdd)
    val flowMultiplyShape = builder.add(flowMultiply)

    flowAddShape ~> flowMultiplyShape

    FlowShape(flowAddShape.in, flowMultiplyShape.out)
  }

  Source(1 to 10).via(flowShape).to(Sink.foreach(println)).run()

}
