package saa

import org.apache.spark.mllib.clustering.KMeans
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.linalg.distributed.RowMatrix
import org.apache.spark.mllib.recommendation.{ALS, Rating}
import org.apache.spark.{SparkConf, SparkContext}

/**
  * author: jinke
  * data: 2017/8/29
  **/
object Regression {
  def main(args: Array[String]): Unit = {
    val sc = new SparkContext(new SparkConf()
      .setAppName("dad22").setMaster("local"))

    val movies = sc.textFile("data/u.item")
    val genres = sc.textFile("data/u.genre")

    val genreMap = genres.filter(!_.isEmpty).map(line =>
      line.split("\\|")).map(array => (array(1), array(0))).collectAsMap()

    //(id, (电影名，类别名))
    val titleAndGenres = movies.map(_.split("\\|")).map {
      array =>
        val genres = array.toSeq.slice(5, array.size)
        val genresAssigned = genres.zipWithIndex.filter { case (g, idx) => g == "1" }.map {
          case (g, idx) => genreMap(idx.toString)
        }
        (array(0).toInt, (array(1), genresAssigned))
    }
    //    println(titleAndGenres.first())
    //获取电影用户的因素向量
    val rawData = sc.textFile("data/u.data")
    val rawRatings = rawData.map(_.split("\t").take(3))
    val ratings = rawRatings.map{case Array(user, movie, rating) =>
      Rating(user.toInt, movie.toInt, rating.toDouble)}.cache()

    val aslModel = ALS.train(ratings, 50, 10 , 0.01)

    //获取movie特征
    val movieFactors = aslModel.productFeatures.map{
      case (id, feature) => (id, Vectors.dense(feature))
    }
    val movieVectors = movieFactors.map(_._2)

    val userFactors = aslModel.userFeatures.map{
      case (id, feature) => (id, Vectors.dense(feature))
    }
    val userVectors = userFactors.map(_._2)

    //归一化,查看是否有离群属性，是否需要归一化
//    val movieMatrix = new RowMatrix(movieVectors)
//    val movieMatrixSummary = movieMatrix.computeColumnSummaryStatistics()
//    val userMatrix = new RowMatrix(userVectors)
//    val userMatrixSummary = userMatrix.computeColumnSummaryStatistics()
//
//    println("movie means: " + movieMatrixSummary.mean)
//    println("movie variance: " + movieMatrixSummary.variance)
//    println("user means: " + userMatrixSummary.mean)
//    println("user variance: " + userMatrixSummary.variance)

    val numCluster = 5
    val numIterations = 20
    //训练次数，取最优值
    val numRuns = 3

    //训练
    val movieClusterModel = KMeans.train(movieVectors, numCluster, numIterations, numRuns)

    movieClusterModel.predict(movieVectors).foreach(println)
  }
}
