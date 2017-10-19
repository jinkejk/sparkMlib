package saa

import org.joda.time.{DateTime, Days}


/**
  * created by: jinkejk
  * date: 9/4/17
  **/
object NYTextCar {

  def main(args: Array[String]): Unit = {
    val datatime = new DateTime(2014, 9, 10, 19, 23)

    println(datatime.dayOfYear().get())


  }

}
