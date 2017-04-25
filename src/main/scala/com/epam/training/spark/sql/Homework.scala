package com.epam.training.spark.sql

import java.sql.Date
import java.time.LocalDate

import org.apache.spark.sql.{DataFrame, Row}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.hive.HiveContext
import org.apache.spark.{SparkConf, SparkContext}

object Homework {
  val DELIMITER = ";"
  val RAW_BUDAPEST_DATA = "data/budapest_daily_1901-2010.csv"
  val OUTPUT_DUR = "output"

  def main(args: Array[String]): Unit = {
    val sparkConf: SparkConf = new SparkConf()
      .setAppName("EPAM BigData training Spark SQL homework")
      .setIfMissing("spark.master", "local[2]")
      .setIfMissing("spark.sql.shuffle.partitions", "10")
    val sc = new SparkContext(sparkConf)
    val sqlContext = new HiveContext(sc)

    processData(sqlContext)

    sc.stop()

  }

  def processData(sqlContext: HiveContext): Unit = {

    /**
      * Task 1
      * Read csv data with DataSource API from provided file
      * Hint: schema is in the Constants object
      */
    val climateDataFrame: DataFrame = readCsvData(sqlContext, Homework.RAW_BUDAPEST_DATA)

    /**
      * Task 2
      * Find errors or missing values in the data
      * Hint: try to use udf for the null check
      */
    val errors: Array[Row] = findErrors(climateDataFrame)
    println(errors)

    /**
      * Task 3
      * List average temperature for a given day in every year
      */
    val averageTemeperatureDataFrame: DataFrame = averageTemperature(climateDataFrame, 1, 2)

    /**
      * Task 4
      * Predict temperature based on mean temperature for every year including 1 day before and after
      * For the given month 1 and day 2 (2nd January) include days 1st January and 3rd January in the calculation
      * Hint: if the dataframe contains a single row with a single double value you can get the double like this "df.first().getDouble(0)"
      */
    val predictedTemperature: Double = predictTemperature(climateDataFrame, 1, 2)
    println(s"Predicted temperature: $predictedTemperature")

  }

  def readCsvData(sqlContext: HiveContext, rawDataPath: String): DataFrame = {
    sqlContext.read
      .option("header", "true")
      .option("delimiter", ";")
      .schema(Constants.CLIMATE_TYPE)
      .csv(rawDataPath)
  }

  def findErrors(climateDataFrame: DataFrame): Array[Row] = {
    val isEmpty = udf((text: String) =>  if (text == null || text.isEmpty) 1 else 0)
    climateDataFrame.select(sum(isEmpty(col("observation_date"))),
      sum(isEmpty(col("mean_temperature"))),
      sum(isEmpty(col("max_temperature"))),
      sum(isEmpty(col("min_temperature"))),
      sum(isEmpty(col("precipitation_mm"))),
      sum(isEmpty(col("precipitation_type"))),
      sum(isEmpty(col("sunshine_hours")))).collect()
  }

  def averageTemperature(climateDataFrame: DataFrame, monthNumber: Int, dayOfMonth: Int): DataFrame = {
    climateDataFrame.select(col("mean_temperature"))
      .where(month(col("observation_date")) === monthNumber)
      .where(dayofmonth(col("observation_date")) === dayOfMonth)
  }

  def predictTemperature(climateDataFrame: DataFrame, monthNumber: Int, dayOfMonth: Int): Double = {
    val observationDateInRange = udf((observationDate: Date) => dateInRange(observationDate.toLocalDate, monthNumber, dayOfMonth))
    climateDataFrame
      .where(observationDateInRange(col("observation_date")))
      .select(avg(col("mean_temperature")))
      .first()
      .getDouble(0)
  }

  private def dateInRange(observationDate: LocalDate, month: Int, dayOfMonth: Int): Boolean = {
    dateMatches(observationDate, month, dayOfMonth) ||
      dateMatches(observationDate.plusDays(1), month, dayOfMonth) ||
      dateMatches(observationDate.minusDays(1), month, dayOfMonth)
  }

  private def dateMatches(observationDate: LocalDate, month: Int, dayOfMonth: Int): Boolean = {
    month == observationDate.getMonthValue && dayOfMonth == observationDate.getDayOfMonth
  }
}


