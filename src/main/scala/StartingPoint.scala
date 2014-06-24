/**
 * Created by Salah on 6/23/2014.
 */
object StartingPoint {
    def main(args: Array[String]): Unit = {
      // starting 2 frontend nodes and 3 backend nodes



      if (args(0)=="consumer")
      {
        GPACalculator.main(Seq(args(1)).toArray)
      }
      else
      {
        GradeGenerator.main(Seq(args(1)).toArray)
      }
      /*GPACalculator.main(Seq("2553").toArray)
      GradeGenerator.main(Seq("2554").toArray)*/
      //TransformationBackend.main(Array.empty)
      //TransformationBackend.main(Array.empty)
      //TransformationFrontend.main(Array.empty)
    }

}


