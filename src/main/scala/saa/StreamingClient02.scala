package saa

import java.text.SimpleDateFormat
import java.util.Date

import org.apache.spark.streaming.{Seconds, StreamingContext}

/**
  * created by: jinkejk
  * date: 8/30/17
  **/
object StreamingClient02 {

  //固定参数格式
  def updateState(prices: Seq[(String, Double)], currentTotal: Option[(Int, Double)]) = {
    val currentRevenue = prices.map(_._2).sum
    val currentNumPurchases = prices.size
    val state = currentTotal.getOrElse((0, 0.0))
    Some((currentNumPurchases + state._1), currentRevenue + state._2)
  }

  def main(args: Array[String]): Unit = {
    //设置批处理间隔10秒
    val ssc = new StreamingContext("local[2]", "first streaming", Seconds(10))
    //对有状态的操作(),需要一个检查点
    ssc.checkpoint("sparkStreamingChectPoint/")
    val stream = ssc.socketTextStream("localhost", 9999)

    val events = stream.map{record =>
      val event = record.split(",")
      (event(0), event(1), event(2).toDouble)
    }

    val users = events.map{ case (user, product, price) => (user,(product, price))}
    val revenuePerUser = users.updateStateByKey(updateState)

    revenuePerUser.print()

    //开始执行
    ssc.start()
    ssc.awaitTermination()
  }
}
