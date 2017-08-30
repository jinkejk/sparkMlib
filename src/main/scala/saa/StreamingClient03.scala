package saa

import breeze.linalg.DenseVector
import org.apache.spark.streaming.{Seconds, StreamingContext}

import scala.util.Random

/**
  * created by: jinkejk
  * date: 8/30/17
  **/
object StreamingClient03 {


  def main(args: Array[String]): Unit = {
    //设置批处理间隔10秒
    val ssc = new StreamingContext("local[2]", "first streaming", Seconds(10))
    //对有状态的操作(),需要一个检查点
    ssc.checkpoint("sparkStreamingChectPoint/")
    val stream = ssc.socketTextStream("localhost", 9999)

    val random = new Random()
    val MaxEvents = 100
    val NumFeatures = 100
    def generateRandomArray(n: Int) = Array.tabulate(n)(_ => random.nextGaussian())

    //生成权重向量
    val w = new DenseVector(generateRandomArray(NumFeatures))
    val intercept = random.nextGaussian() * 10

    //生成随机点
    def generateNoisyData(n: Int) = {
      (1 to n).map{ i =>
        val x = new DenseVector(generateRandomArray(NumFeatures))
        val y: Double = w.dot(x)
        val noisy = y + intercept
        (noisy, x)
      }
    }
    val events = stream.map{record =>
      val event = record.split(",")
      (event(0), event(1), event(2).toDouble)
    }


    //开始执行
    ssc.start()
    ssc.awaitTermination()
  }
}
