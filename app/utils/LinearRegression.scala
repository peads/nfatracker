package utils

import scala.io.Source.fromURL
import scala.util.{Success, Try}
import scala.collection.JavaConverters
import com.univocity.parsers.common.processor.RowListProcessor
import com.univocity.parsers.csv.CsvParser
import com.univocity.parsers.csv.CsvParserSettings
import dal.RowRepository
import org.joda.time.{DateTime, Days}
import smile.regression.ols
import scala.concurrent.Await

trait LinearRegression {

  protected val NFA_TRACKER_URL = "http://www.nfatracker.com/wp-content/themes/smartsolutions/inc/export/"
  protected val INCLUDED_HEADERS = List("NFAItem", "FormType", "Approved", "CheckCashed")
  protected val DURATION = scala.concurrent.duration.Duration(30, scala.concurrent
    .duration.SECONDS)
  // protected val NFA_ITEM_TYPES = List("Suppressor", "SBR", "SBS", "MG", "AOW")

  protected def outOfRangeFilter(checkCashedDate: Double, approvedDate: Double)
  : Boolean = {
    val timeDiff = approvedDate - checkCashedDate
    approvedDate > 0 && checkCashedDate > 0 && timeDiff >= 14 && timeDiff < 1000
  }
  protected def normalizedTimeStamp(baseDate: DateTime, date: DateTime) : Double =
    Days.daysBetween(baseDate, date).getDays.toDouble

  protected def predict(repo: RowRepository)(baseDate: DateTime, date: DateTime, nfaType: String): String = {
    val dateDouble = normalizedTimeStamp(baseDate, date)

    val dbResult = Await.result(repo.list(), DURATION).filter(_.nfaItem.contains
    (nfaType))
      .map(row => (normalizedTimeStamp(baseDate, new DateTime(row
        .checkCashedDate)), normalizedTimeStamp(baseDate, new DateTime(row
        .approvedDate))))
      .filter{ case (c, a) => outOfRangeFilter(c,a)}
      .toArray

    val (x, y) = dbResult.unzip

    // add constant to explanatory variables, and create model
    val model = ols(x.map(Array(1, _)),y)

    val prediction = model.predict(Array(1, dateDouble))

    baseDate.toLocalDate.plusDays(prediction.floor.toInt).toString()
  }

  protected def generateData(url: String): (Array[String],
    List[Array[String]]) = {
    val reader = fromURL(url).reader()

    // The settings object provides many configuration options// The settings object provides many configuration options
    val parserSettings = new CsvParserSettings

    //You can configure the parser to automatically detect what line separator sequence is in the input
    parserSettings.setLineSeparatorDetectionEnabled(true)

    // A RowListProcessor stores each parsed row in a List.
    val rowProcessor = new RowListProcessor

    // You can configure the parser to use a RowProcessor to process the values of each parsed row.
    // You will find more RowProcessors in the 'com.univocity.parsers.common.processor' package, but you can also create
    // your own.
    parserSettings.setProcessor(rowProcessor)

    // Let's consider the first parsed row as the headers of each column in the file.
    parserSettings.setHeaderExtractionEnabled(true)

    // creates a parser instance with the given settings
    val parser = new CsvParser(parserSettings)

    // the 'parse' method will parse the file and delegate each parsed row to the RowProcessor you defined
    parser.parse(reader)

    // get the parsed records from the RowListProcessor here.
    // Note that different implementations of RowProcessor will provide different sets of functionalities.
    (rowProcessor.getHeaders, JavaConverters.asScalaBuffer(rowProcessor.getRows).toList)
  }

  protected def filterData(headers: Array[String], rows: List[Array[String]]):List[
    (String, String, DateTime, DateTime)] = {
    val includedHeadersIdx = INCLUDED_HEADERS.map(headers.indexOf(_))
    rows.map(row =>
      // filter unnecessary data
      row.zipWithIndex.filter {
        case (_, i) => includedHeadersIdx.contains(i)
      }.unzip._1.filter(_ != null)
      // filter missing data, and Form 3's
    ).filter(_.length > 3).filterNot(_.contains("Form 3 To Dealer"))
    .map(row =>
      row.zipWithIndex.map{
        // apply parse on date iff column contains date
        // otherwise wrap cell in Success for later filtering
        case(s, i) => if (i < 2) Success(s) else Try(DateTime.parse(s))
        // filter out failed parses
      }.filter(_.isSuccess).map(_.get)
      // filter incomplete data
    ).filter(_.length > 3).map(r => (
      r(0).toString,
      r(1).toString,
      r(2).asInstanceOf[DateTime],
      r(3).asInstanceOf[DateTime])
      // filter nonsensical dates
    ).filter{case(_,_,checkCashed,approved) => approved.isAfter(checkCashed)}
  }
}