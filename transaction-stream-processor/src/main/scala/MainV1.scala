
import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.functions._
import org.apache.spark.sql.streaming.Trigger
import org.apache.spark.sql.types._
import org.apache.spark.sql.{Column, SparkSession}
object MainV1 {

  def main1(args: Array[String]): Unit = {

    val spark = SparkSession.builder().master("local[1]").getOrCreate()

    spark.conf.set("spark.sql.streaming.forceDeleteTempCheckpointLocation", "true")
    spark.conf.set("spark.sql.streaming.checkpointLocation", "/Users/dshingav/tmp/")


    val mySchema = StructType(Array(
      StructField("account_no", StringType),
      StructField("date", TimestampType),
      StructField("transaction_details", StringType),
      StructField("value_date", TimestampType),
      StructField("transaction_type", StringType),
      StructField("amount", LongType),
      StructField("balance", LongType),

    ))

    val df = spark
      .readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "kafka:9092")
      .option("subscribe", "transaction-logs")
      //      .schema(mySchema)
      .load()

    //      .csv("/Users/dshingav/Downloads/bank_data.csv")
    //      .withColumn("date", col("date").cast("timestamp"))

    val w = Window
      .partitionBy(col("account_no"))
      .orderBy(col("date").cast("timestamp").cast("long"))
      .rangeBetween(-3600 * 24 * 30, Window.currentRow)


    val colMap: Array[Column] = mySchema
      .fields
      .zipWithIndex
      .map { case (columnInfo: StructField, i: Int) => {
        split(col("value"), ",").getItem(i).cast(columnInfo.dataType).as(columnInfo.name)
      }
      }

    df.
      select(colMap: _*)
      .withColumn("sumAmount"
        , sum(col("amount").cast(DecimalType(18, 2)))
          .over(w))
      .orderBy("account_no", "date")
      .filter(col("sumAmount") > 1000000000)
      .select(lit("output-topic").as("topic"), col("account_no").as("key"),
        concat_ws(",", col("account_no"), col("sumAmount")).as("value"))
      .distinct()
      .selectExpr("topic",
        "CAST(key AS STRING)",
        "CAST(value AS STRING)")
      .writeStream
      .option("checkpointLocation", spark.conf.get("spark.sql.streaming.checkpointLocation"))
      .trigger(Trigger.ProcessingTime("1 minute"))
      .format("kafka")
      .option("kafka.bootstrap.servers", "kafka:9092")
      .start()
    /*.foreachBatch( (batch : Dataset[Row], id : Long) => {
//         println()
      batch.foreach(r => {
        println(s"account_no=${r.get(1)} sumAmount=${r.get(2)}")
      })
    })*/


    //    println(finalDf.count())

  }

}
