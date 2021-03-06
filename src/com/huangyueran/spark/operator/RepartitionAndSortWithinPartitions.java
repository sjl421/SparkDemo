package com.huangyueran.spark.operator;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.spark.Partitioner;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.api.java.function.VoidFunction;

import scala.Tuple2;

/**
 * @category 根据给定的Partitioner重新分区，并且每个分区内根据comp实现排序。
 * @author huangyueran
 * @time 2017-7-21 16:38:20
 */
public class RepartitionAndSortWithinPartitions {

	public static void main(String[] args) {
		/**
		 * SparkConf:第一步创建一个SparkConf，在这个对象里面可以设置允许模式Local Standalone yarn
		 * AppName(可以在Web UI中看到) 还可以设置Spark运行时的资源要求
		 */
		SparkConf conf = new SparkConf().setAppName("RepartitionAndSortWithinPartitions").setMaster("local");
		// SparkConf conf = new SparkConf().setAppName("JoinOperator");

		/**
		 * 基于SparkConf的对象可以创建出来一个SparkContext Spark上下文
		 * SparkContext是通往集群的唯一通道，SparkContext在创建的时候还会创建任务调度器
		 */
		JavaSparkContext sc = new JavaSparkContext(conf);

		repartitionAndSortWithinPartitions(sc);
	}

	static void repartitionAndSortWithinPartitions(JavaSparkContext sc) {
		List<String> datas = new ArrayList<>();
		Random random = new Random(1);
		for (int i = 0; i < 10; i++) {
			for (int j = 0; j < 100; j++) {
				datas.add(String.format("product%02d,url%03d", random.nextInt(10), random.nextInt(100)));
			}
		}
		JavaRDD<String> datasRDD = sc.parallelize(datas);

		JavaPairRDD<String, String> pairRDD = datasRDD.mapToPair(new PairFunction<String, String, String>() {

			@Override
			public Tuple2<String, String> call(String v) throws Exception {
				String[] values = v.split(",");
				return new Tuple2<>(values[0], values[1]);
			}
		});
		// pairRDD.repartition(1000); // 该函数其实就是coalesce函数第二个参数为true的实现

		JavaPairRDD<String, String> partSortRDD = pairRDD.repartitionAndSortWithinPartitions(new Partitioner() {

			@Override
			public int numPartitions() {
				return 10;
			}

			@Override
			public int getPartition(Object key) {
				return Integer.valueOf(((String) key).substring(7));
			}
		});
		partSortRDD.collect();
		partSortRDD.foreach(new VoidFunction<Tuple2<String, String>>() {

			@Override
			public void call(Tuple2<String, String> v) throws Exception {
				System.out.println("v:" + v._1 + "\tindex:" + v._2);

			}
		});
	}

}
