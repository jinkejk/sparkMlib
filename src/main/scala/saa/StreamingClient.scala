package saa

import java.text.SimpleDateFormat
import java.util.Date

import org.apache.spark.streaming.{Seconds, StreamingContext}

/**
  * created by: jinkejk
  * date: 8/30/17
  **/
object StreamingClient {

  def main(args: Array[String]): Unit = {
    //设置批处理间隔10秒
    val ssc = new StreamingContext("local[2]", "first streaming", Seconds(10))
    val stream = ssc.socketTextStream("localhost", 9999)

    val events = stream.map{record =>
      val event = record.split(",")
      (event(0), event(1), event(2))
    }

    //无状态的操作
    events.foreachRDD{(rdd, time) =>
      val numPurchase = rdd.count()
      val uniqueUsers = rdd.map{case (user, _, _) => user}.distinct().count()
      val totalRevenue = rdd.map{case (_, _, price) => price.toDouble}.sum()
      val productPopularity = rdd.map{case (user, product, price) => (product, 1)}
        .reduceByKey(_ + _)
        .collect()
        .sortBy(-_._2)
      val mostPopular = productPopularity(0)

      val formatter = new SimpleDateFormat
      val dataStr = formatter.format(new Date(time.milliseconds))
      println(s"===Batch start time: $dataStr ====")
      println(s"total purchases: $numPurchase")
      println(s"unique Users: $uniqueUsers")
      println(s"Total revenue: $totalRevenue")
      println(s"Most popular product: ${mostPopular._1} || with ${mostPopular._2} purchases")
    }

    //开始执行
    ssc.start()
    ssc.awaitTermination()
  }
}
