/**
 * Created by Salah on 6/10/2014.
 */


import akka.actor._
import akka.pattern.{ ask, pipe }
import akka.actor.SupervisorStrategy._
import com.typesafe.config.ConfigFactory
import akka.event.{Logging, LoggingReceive}
import akka.pattern.{ ask, pipe }
import scala.collection.parallel.immutable
import scala.collection._
import scala.concurrent.duration._
import akka.persistence._
import scala.concurrent.{Future, Await}
import akka.util.Timeout
import akka.event.Logging


object Exception_Msgs
{
  class Exception_Msg(msg: String) extends RuntimeException(msg)
}
case class tods(value: Int)
case object Begin
case object Request

case class Evt (sal: Int)
case class Cmd (step: Int)

case class Result(ctr: Int)


class rsvr extends EventsourcedProcessor {

  println("Intilaization")
  var counter  =0;
  def updateState(event: Evt): Unit =
  counter = event.sal

  val receiveRecover: Receive = {

    case evt: Evt =>
      println("sd")
      updateState(evt)
      //case SnapshotOffer(_, snapshot: ExampleState) => state = snapshot
}
  val receiveCommand: Receive = {
    case a @ Cmd(step) =>
      //TODO switch to immutable
      counter = counter + a.step
      persist(Evt(counter)) {
        event => updateState(event)
          context.system.eventStream.publish(event)}
      // the message the cause the error will not be persisted..
      if(counter==20)  {
        sender() ! "Stop"
        throw new Exception_Msgs.Exception_Msg("The counter reached its Maximum: i.e, 20")
      }
    case "Request" => sender() ! counter
    case "snap"  => saveSnapshot(counter)
    case _ => sender() ! println("I do not understand")
  }
}
class sndr extends Actor {
  val child = context.actorOf(Props[rsvr],"EventSorucer")
  override val supervisorStrategy = OneForOneStrategy() {
    case _: Exception_Msgs.Exception_Msg => Restart
  }

  case class Res (x: Int)
  def receive = {
    case "Add5" => child ! Cmd(step = 5)
    case "Add6" => child ! Cmd(step = 6)
    case "Request" =>
      implicit val timeout = Timeout(10 seconds)
      val future = child.ask("Request")(10 seconds) // enabled by the “ask” import
      val result = Await.result(future, timeout.duration).asInstanceOf[Int]
      sender() ! result
    case _ => println("Stop Working......")
  }
}
object ActorEventSourcing extends App {

  val system = ActorSystem("Mysystem")
  val act = system.actorOf(Props[sndr],"Sender")
  act ! "Add5"
  Req
  act ! "Add5"
  act ! "Add5"
  act ! "Add5"
  Thread.sleep(500)
  act ! "Add6"
  Req

  def Req = {
    implicit val timeout = Timeout(5 seconds)
    val future: Future[Any] = act.ask("Request")(5 seconds) // enabled by the “ask” import
    val result = Await.result(future, timeout.duration).asInstanceOf[Int]
    println(">>>>>>>  " + result)
  }

}
