package saa

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

import org.apache.spark.mllib.feature.StandardScaler
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.linalg.distributed.RowMatrix
import org.apache.spark.{SparkConf, SparkContext}

/**
  * author: jinke
  * data: 2017/8/29
  **/
object PCA {
  def main(args: Array[String]): Unit = {
    val sc = new SparkContext(new SparkConf()
      .setAppName("dad22").setMaster("local"))

    val rdd = sc.wholeTextFiles("image/*.jpg")
    //去掉file:
    val files = rdd.map{ case (fileName, content) =>
      fileName.replace("file:", "")
    }

    //像素特征
    val pixels = files.map(f => extractPixels(f, 50, 50))
    val vectors = pixels.map(Vectors.dense(_))

    //PCA前的正规化
    val scaler = new StandardScaler(withMean = true, withStd = false).fit(vectors)
    //减去当前列的平均值
//    val scaledVector = vectors.map(scaler.transform(_))
    val scaledVector = scaler.transform(vectors)

    //PCA
    val matrix = new RowMatrix(scaledVector)
    //得到的是前10个主成分特征(2500*10)
    val pc = matrix.computePrincipalComponents(10)
    //投影
    val projected = matrix.multiply(pc)
    projected.rows.foreach(p => println(p.toArray.mkString(",")))
  }

  //加载图片
  def loadImageFromFile(path: String): BufferedImage = {
    ImageIO.read(new File(path))
  }

  //转换为灰度图片
  def processImage(image: BufferedImage, width: Int, height: Int): BufferedImage = {
    val bwImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY)
    val g = bwImage.getGraphics

    //开始转换
    g.drawImage(image, 0, 0, width, height, null)
    g.dispose()
    bwImage
  }

  //获取图片像素特征
  def getPixelFromImage(image: BufferedImage): Array[Double] = {
    val pixels = Array.ofDim[Double](image.getWidth*image.getHeight)

    image.getData.getPixels(0, 0, image.getWidth, image.getHeight, pixels)
  }

  //整合到一起
  def extractPixels(path: String, width: Int, height: Int): Array[Double] = {
      val raw = loadImageFromFile(path)
      val process = processImage(raw, width, height)
      getPixelFromImage(process)
  }
}
