package saa

import java.io.PrintWriter
import java.net.ServerSocket

import scala.io.Source
import scala.util.Random

/**
  * created by: jinkejk
  * date: 8/30/17
  **/
object StreamingServer {

  def main(args: Array[String]): Unit = {
    val random = new Random()
    val MaxEvent = 6

    //添加为资源文件后 用该路径
    val nameResource = this.getClass.getResourceAsStream("/names.csv")
    val names = Source.fromInputStream(nameResource)
      .getLines()
      .toList
      .head
      .split("\\W+")
      .toSeq

    println(names)
    //产品序列,虚拟
    val products = Seq(
      "iphone" -> 99.9,
      "Headphones" -> 5.49,
      "Samsung Galaxy Cover" -> 8.95,
      "ipad cover" -> 7.49

    )

    val listener = new ServerSocket(9999)
    println("Listening on port: 9999")

    //处理请求,单线程
    while(true){
      val socket = listener.accept()
      new Thread(){
        override def run = {
          println("Got client connect from: " + socket.getInetAddress)
          val out = new PrintWriter(socket.getOutputStream, true)

          //每隔一秒写一次,写入流内
          while(true){
            Thread.sleep(1000)
            val num = random.nextInt(MaxEvent)
            val productEvent = generateProductEvent(num)

            productEvent.foreach{event =>
              out.write(event.productIterator.mkString(","))
              out.write("\n")
            }

            out.flush()
            println(s"Created $num events....")
          }
        }
      }.start()
    }

    //映射名字到产品
    def generateProductEvent(n: Int) ={
      (1 to n).map{i =>
        val (product, price) = products(random.nextInt(products.size))
        //shuffle: 将元素随机排序
        val user = random.shuffle(names).head

        (user, product, price)
      }
    }
  }



}
