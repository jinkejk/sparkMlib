package saa

import org.apache.spark.mllib.evaluation.BinaryClassificationMetrics
import org.apache.spark.mllib.feature.{HashingTF, IDF, StandardScaler}
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.tree.DecisionTree
import org.apache.spark.{SparkConf, SparkContext}

object test {

  case class Article(title:String, body:String, url: String)
  //这个必须放在这个位置

  def main(args: Array[String]): Unit = {
    val sc = new SparkContext(new SparkConf()
      .setAppName("dad22").setMaster("local"))

    val rawData = sc.textFile("data/train.tsv")

    val records = rawData.map(_.split("\t"))

    //加载文档,一行是一个文档
    //读取json信息, 调整格式双引号变为单引号, 去掉首尾的引号
    val articleRDD = records.map{p=> p(2).replaceAll("\"", "").split(" ").toSeq}

    //HashingTF是一个Transformer，文本处理中接收词条的集合然后把这些集合转换成固定长度的特征向量
    //这个算法在哈希的同时会统计各个词条的词频
    val hashingTF = new HashingTF(150)
    val tf = hashingTF.transform(articleRDD).cache()

    //输出桶 hash码, 频率
//    tf.take(2).foreach(println)

    val idf=new IDF().fit(tf)
    val tfidf =idf.transform(tf).zipWithIndex().map(x => (x._2, x._1.toArray))

    //数值特征
    val features = records.map { r =>
      val trimmed = r.map(_.replaceAll("\"", ""))
      val label = trimmed(r.size - 1).toInt
      //特征包括最后一位
      val feature = trimmed.slice(4, r.size - 1).map(d=> if(d == "?") 0.0 else d.toDouble)
      (label,feature)
    }.zipWithIndex().map(x => (x._2, x._1)).cache()

    //数值特征
    val labelPoint = tfidf.join(features).map(p =>
      LabeledPoint(p._2._2._1, Vectors.dense(p._2._1 ++ p._2._2._2)))

    val model = DecisionTree.trainClassifier(labelPoint, 2, Map[Int, Int](), "gini", 5, 32)

//    model.save(sc, "model/")

    // Evaluate model on test instances and compute test error
    val labelAndPreds = labelPoint.map { point =>
      val prediction = model.predict(point.features)
      (prediction, point.label)
    }

    val accuracy = labelPoint.map(point =>
      if(model.predict(point.features) == point.label) 1 else 0
    ).sum() / labelPoint.count()
    println(s"ACC: ${accuracy}")
    val mtri = new BinaryClassificationMetrics(labelAndPreds)
    println(s"PR: ${mtri.areaUnderPR()}\n ROC: ${mtri.areaUnderROC()}")

    //标准化
//    val vector = labelPoint.map(lp => lp.features)
//    val scaler = new StandardScaler(withMean = true, withStd = true).fit(vector)
//    val scaledData = labelPoint.map(lp => LabeledPoint(lp.label, scaler.transform(lp.features)))
//
//    println(labelPoint.first.features)
//    println(scaledData.first().features)
  }
}
