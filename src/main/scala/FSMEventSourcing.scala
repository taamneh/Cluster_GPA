/**
 * Created by Salah on 6/18/2014.
 */
/**
 * Created by Salah on 6/12/2014.
 */
import akka.actor.SupervisorStrategy._
import akka.actor._
import akka.pattern.ask
import akka.persistence._
import akka.util.Timeout

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}



//

class GPA_Calulation_ES extends EventsourcedProcessor with FSM[State, Data] with akka.actor.ActorLogging {
  def Rank(cls: Any)= {
    cls match {
      case x :Double if(x >=90 )  => Excellent
      case x: Double if(x >=80 )  => Good
      case x: Double if(x >=70 )  => Satisfactory
      case _  => Poor
    }
  }

  /*override def receive =
  {

    case evt: Evt                                 => println("recover")
    case _  =>
      println("ma3gool")
    //case SnapshotOffer(_, snapshot: ExampleState) => state = snapshot
  }*/

  startWith(None, GPA(ctr=0, tot_hr =0))
  override val receiveRecover: Receive =
  {
    case evt: stateAndData                                 =>
      updateState(evt)
      startWith(evt.st, evt.dt)
    case _ => println("...........Event Sourcing..........")
    //case SnapshotOffer(_, snapshot: ExampleState) => state = snapshot
  }
  override val receiveCommand: Receive = {
    case Cmd(data) => println("Salah")
  }
  val center = stateAndData(None, GPA(ctr=0, tot_hr =0))
  def updateState(event: stateAndData): Unit = {
    center.copy(st = event.st, dt = event.dt)
  }
  when(None) {
    case Event(Grade(grd, hr),_) =>
      persist(stateAndData(Rank(grd.toDouble), GPA(ctr = grd, tot_hr = hr))) {
        event => updateState(event)
          context.system.eventStream.publish(event)
      }
      goto(Rank(grd.toDouble)) using GPA(ctr = grd, tot_hr = hr)
  }
  when(Excellent) {
    case Event(Grade(grd, hr),t @ GPA(ctr, tot_hr)) =>
      val temp = (t.ctr*t.tot_hr + grd*hr)/(hr+t.tot_hr)
      persist(stateAndData(Rank(grd.toDouble), GPA(ctr = temp, tot_hr = (hr+t.tot_hr)))) {
        event => updateState(event)
          context.system.eventStream.publish(event)
      }
      goto(Rank(temp)) using t.copy(ctr = (ctr*tot_hr + grd*hr)/(hr+tot_hr),tot_hr+hr )
  }
  when(Good) {
    case Event(Grade(grd, hr),t @ GPA(ctr, tot_hr)) =>
      val temp = (t.ctr*t.tot_hr + grd*hr)/(hr+t.tot_hr)
      persist(stateAndData(Rank(grd.toDouble), GPA(ctr = temp, tot_hr = (hr+t.tot_hr)))) {
        event => updateState(event)
          context.system.eventStream.publish(event)
      }
      goto(Rank(temp)) using t.copy(ctr = (ctr*tot_hr + grd*hr)/(hr+tot_hr),tot_hr+hr )
  }
  when(Satisfactory) {
    case Event(Grade(grd, hr),t @ GPA(ctr, tot_hr)) =>
      val temp = (t.ctr*t.tot_hr + grd*hr)/(hr+t.tot_hr)
      persist(stateAndData(Rank(grd.toDouble), GPA(ctr = temp, tot_hr = (hr+t.tot_hr)))) {
        event => updateState(event)
          context.system.eventStream.publish(event)
      }
      goto(Rank(temp)) using t.copy(ctr = (ctr*tot_hr + grd*hr)/(hr+tot_hr),tot_hr+hr )
  }
  when(Poor) {
    case Event(Grade(grd,hr),t @ GPA(ctr, tot_hr)) =>
      val temp = (t.ctr*t.tot_hr + grd*hr)/(hr+t.tot_hr)
      persist(stateAndData(Rank(grd.toDouble), GPA(ctr = temp, tot_hr = (hr+t.tot_hr)))) {
        event => updateState(event)
          context.system.eventStream.publish(event)
      }
      goto(Rank(temp)) using t.copy(ctr = (ctr*tot_hr + grd*hr)/(hr+tot_hr),tot_hr+hr )

  }

  onTransition {
    case None -> Excellent =>  log.debug("TRANSAITION: None -> Excellent")
    case None -> Good => log.debug("TRANSAITION: None -> Good")
    case None -> Satisfactory => log.debug("TRANSAITION: None -> Satisfactory")
    case None -> Poor  => log.debug("TRANSAITION: None -> Poor")
    case Excellent -> Good => log.debug("TRANSAITION: Excellent -> Good")
    case Excellent -> Satisfactory => log.debug("TRANSAITION: Excellent -> Satisfactory")
    case Excellent -> Poor => log.debug("TRANSAITION: Excellent -> Poor")
    case Good -> Excellent => log.debug("TRANSAITION: Good -> Excellent")
    case Good -> Satisfactory => log.debug("TRANSAITION: Good -> Satisfactory")
    case Good -> Poor => log.debug("TRANSAITION: Good -> Poor")
    case Satisfactory -> Excellent => log.debug("TRANSAITION: Satisfactory -> Excellent")
    case Satisfactory -> Good => log.debug("TRANSAITION: Satisfactory -> Good")
    case Satisfactory -> Poor => log.debug("TRANSAITION: Satisfactory -> Poor")
    case Poor -> Excellent => log.debug("TRANSAITION: Poor -> Excellent")
    case Poor -> Good => log.debug("TRANSAITION: Poor -> Good")
    case Poor -> Satisfactory => log.debug("TRANSAITION: Poor -> Satisfactory")
  }
  whenUnhandled {
    case Event("Request", s) =>
      sender ! stateData
      stay
    case Event("Throw", _) =>
      log.debug("Failure Happend.....Restarting")
      throw new Exception_Msgs.Exception_Msg("Exception..We are going to restart")
    case Event(e, s) => println("Nothing happen")
      stay
  }
  override def preRestart(reason: Throwable, message: Option[Any]) {
    message match {
      case Some(p: Persistent) if !recoveryRunning => deleteMessage(p.sequenceNr) // mark failing message as deleted
      case _                                       => // ignore
    }
    super.preRestart(reason, message)
  }
  initialize()

  override def postRestart(reason: Throwable): Unit = {
    //preStart()
    log.debug("Finished Restarting>>>>>>> back to work")
  }

}

class sndr_EV extends Actor {

  // Stop the CounterService child if it throws ServiceUnavailable

  override val supervisorStrategy = OneForOneStrategy() {
    case _: Exception_Msgs.Exception_Msg =>
      Restart

  }

  val test = context.actorOf(Props[GPA_Calulation_ES],"Test")
  def receive = {
    case Grade(grd, hr)=>  test ! Grade(grd, hr)
    case "Request" =>
      implicit val timeout = Timeout(5 seconds)
      val future: Future[Any] = test.ask("Request")(5 seconds) // enabled by the “ask” import
    val result = Await.result(future, timeout.duration).asInstanceOf[GPA]
      sender() ! result.ctr
    case "Throw" => test ! "Throw"
    case _ =>
      println("Stop Working......")
  }
}

object FSMEventSourcing extends App{
  val system = ActorSystem("Mysystem")
  val act = system.actorOf(Props[sndr_EV],"Sender")
  Thread.sleep(500)
  var inputGrd = ""
  do {
    println("Plese enter a grade of Exit")
    inputGrd = Console.readLine()
    if (inputGrd == "Throw")
    {
      act ! "Throw"
      Thread.sleep(500)
    }
    else if(inputGrd == "Request")
    {
      implicit val timeout = Timeout(5 seconds)
      val future: Future[Any] = act.ask("Request")(5 seconds) // enabled by the “ask” import
    val result = Await.result(future, timeout.duration).asInstanceOf[Double]
      println("Current GPA:" + result)
    }
    else if (inputGrd != "Exit" && inputGrd !="") {
      implicit val timeout = Timeout(5 seconds)
      act ! Grade(inputGrd.toInt, 3) // enabled by the “ask” import
      val future: Future[Any] = act.ask("Request")(5 seconds) // enabled by the “ask” import
      val result = Await.result(future, timeout.duration).asInstanceOf[Double]
      println("Current GPA:" + result)
    }
  } while (inputGrd != "Exit")
  Thread.sleep(500)
  system.shutdown()
}
