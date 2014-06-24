/**
 * Created by Salah on 6/12/2014.
 */
import akka.actor._
import akka.actor.SupervisorStrategy._
import com.typesafe.config.ConfigFactory
import akka.event.LoggingReceive
import akka.pattern.{ ask, pipe }
import scala.collection.parallel.immutable
import scala.collection._
import scala.concurrent.duration._
import akka.persistence._

//
/*sealed trait State
case object Idle extends State
case object Active extends State

sealed trait Data
case object Uninitialized extends Data
case class Datastore (ctr : Int) extends Data*/

case class stateAndData (st: State, dt: Data)
       //ds.copy(ctr = ctr + value)
//def updateDataStore(ds: Datastore, value: Int): Datastore =
         //ds.copy(ctr = ctr + value)


class TestFsm3 extends Processor with FSM[State, Data] with EventsourcedProcessor {

  println("Welcome")
  override def receive =
  {
    case evt: Evt                                 => println("recover")
    //case SnapshotOffer(_, snapshot: ExampleState) => state = snapshot
  }
  override val receiveRecover: Receive =
  {
    case evt: Evt                                 => println("recover")
    //case SnapshotOffer(_, snapshot: ExampleState) => state = snapshot
  }
  override val receiveCommand: Receive = {
    case Cmd(data) => println("Salah")
  }

  val center = stateAndData(Idle, Uninitialized)
  def updateState(event: stateAndData): Unit =
        center.copy(st = event.st,dt =event.dt )

  startWith(Idle, Uninitialized)
    when(Idle) {
      case Event(Begin, Uninitialized) =>
        println("We move to Active State")
        persist(stateAndData(Active, Datastore(0))) {
          event => updateState(event)
            context.system.eventStream.publish(event)
        }
        goto(Active) using Datastore(0)
      /*persist(Evt(counter)) {
        event => updateState(event)
          context.system.eventStream.publish(event)}*/

      case Event(tods(value), _) =>
        println("We cant accept your Message now please try later")
        stay()
    }

    // transition elided ...
      when(Active) {
        case Event(tods(value), t@Datastore(ctr)) =>
          println("We are in Active ")
          persist(stateAndData(Active, Datastore(ctr + value))) {
            event => updateState(event)
              context.system.eventStream.publish(event)
          }
          stay() using t.copy(ctr = ctr + value)
      }
    whenUnhandled {
      case Event(Request, s) =>
        sender ! stateData
        stay
      case Event("Throw", _) =>
        throw new Exception_Msgs.Exception_Msg("CounterService not available, lack of initial value")
      case Event(e, s) => println("Nothing happen")
        stay

    }
  }

class sndr3 extends Actor {

  // Stop the CounterService child if it throws ServiceUnavailable

  override val supervisorStrategy = OneForOneStrategy() {
    case _: Exception_Msgs.Exception_Msg => Restart
  }

  def receive = {
    case d @ Datastore(ctr) => println(ctr)
    case _ => println("Stop Working......")
  }

  val test = context.actorOf(Props[TestFsm3],"Test")

  // with command sourcing
  println("Str")
  test ! Begin
  println("Str2")
  test ! tods(17)
  test ! tods(18)
  test ! tods(19)
  test ! tods(6)
  test ! Request
  test ! "Throw"
  test ! tods(6) // 6 will be added to the previous save value
  test ! Request

}

object FMSEventSourcing extends App{
  val system = ActorSystem("Mysystem")
  val act = system.actorOf(Props[sndr3],"Sender")

}
