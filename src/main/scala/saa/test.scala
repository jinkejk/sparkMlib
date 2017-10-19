package saa

case class Level(level: Int) {
  override def toString: String = {
    level.toString
  }
}

object test {
  implicit val level2 = new Level(6)

  def toWork(name: String)(implicit l: Level): Unit = {
    println(s"$name: $l")
  }

  //只能有一个参数,必须要有主构造器
  implicit class Level2(name: String){
    def aaaa(str: String): String ={
      name + str;
    }
  }

  def main(args: Array[String]): Unit = {
    toWork("jinek")
    toWork("jinke")(new Level(2))

    //隐式调用Level2 构造器
    println("abc".aaaa("daaada"));
  }
}
