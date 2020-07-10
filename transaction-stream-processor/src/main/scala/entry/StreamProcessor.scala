package entry

import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.sql.streaming.Trigger
import org.apache.spark.sql.types._

object StreamProcessor  {

  val logger = Logger.getLogger(StreamProcessor.getClass)

  def main(args: Array[String]): Unit = {
    logger.info("Stream processor start ")
    val BROKER_LIST = args(0)
    val SPARK_MASTER = "local[1]"

    Logger.getLogger("org.apache.spark").setLevel(Level.WARN)
    Logger.getLogger("org.spark-project").setLevel(Level.WARN)
    Logger.getLogger("org.apache.kafka").setLevel(Level.WARN)

    val spark = SparkSession
      .builder()
      .getOrCreate()

    logger.info("Spark session created .... ")

    spark.conf.set("spark.sql.streaming.forceDeleteTempCheckpointLocation", "true")
    spark.conf.set("spark.sql.streaming.checkpointLocation", "/app/")


    logger.info(" schema created... ")

    /*val mySchema = StructType(Array(
      StructField("account_no", StringType),
      StructField("date", TimestampType),
      StructField("transaction_details", StringType),
      StructField("value_date", TimestampType),
      StructField("transaction_type", StringType),
      StructField("amount", LongType),
      StructField("balance", LongType),

    ))

    val colMap: Array[Column] = mySchema
      .fields
      .zipWithIndex
      .map { case (columnInfo: StructField, i: Int) => {
        split(col("value"), ",").getItem(i).cast(columnInfo.dataType).as(columnInfo.name)
      }
      }*/

    logger.info("Starting the streaming job .... ")

    spark
      .readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", BROKER_LIST)
      .option("subscribe", "transaction-logs")
      .load()
      .select(split(col("value"), ",").getItem(0).cast(StringType).as("account_no"),
        split(col("value"), ",").getItem(1).cast(TimestampType).as("date"),
        split(col("value"), ",").getItem(2).cast(StringType).as("transaction_details"),
        split(col("value"), ",").getItem(3).cast(TimestampType).as("value_date"),
        split(col("value"), ",").getItem(4).cast(StringType).as("transaction_type"),
        split(col("value"), ",").getItem(5).cast(LongType).as("amount"),
        split(col("value"), ",").getItem(6).cast(LongType).as("balance"))
      .withWatermark("date", "10 minutes")
      .groupBy(col("account_no"), window(col("date"), "30 days"))
      .agg(sum(col("amount").cast(DecimalType(18, 2))).as("sumAmount"))
      .filter(col("sumAmount") > 1000000000)
      .select(lit("output-topic").as("topic"), col("account_no").as("key"),
        concat_ws(",", col("account_no"), col("sumAmount")).as("value"))
      .selectExpr("topic",
        "CAST(key AS STRING)",
        "CAST(value AS STRING)")
      .writeStream
      .option("checkpointLocation", spark.conf.get("spark.sql.streaming.checkpointLocation"))
      .trigger(Trigger.ProcessingTime("1 minute"))
      .format("kafka")
      .option("kafka.bootstrap.servers", BROKER_LIST)
      .start()
      .awaitTermination()

    logger.info("Stream processor terminated ")

  }

}
