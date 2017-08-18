import java.awt.Color
import scala.io.Source.fromURL
import scala.util.Try
import scala.collection.JavaConverters
import com.univocity.parsers.common.processor.RowListProcessor
import com.univocity.parsers.csv.CsvParser
import com.univocity.parsers.csv.CsvParserSettings
import com.github.nscala_time.time.Imports._
import org.joda.time.Days
import smile.regression.Operators

case class Config(baseDate: String = "", date: String= "" , itemType: String= "", verbose: Boolean = false,  plot: Boolean = false)

object LinearRegression extends App with Operators{

  private val NFATRACKER_URL = "http://www.nfatracker.com/wp-content/themes/smartsolutions/inc/export/"
  private val INCLUDED_HEADERS = List("NFAItem", "FormType", "Approved", "CheckCashed")

  def generateData(url: String): (Array[String], List[Array[String]]) = {
    val reader = fromURL(url).reader()

    // The settings object provides many configuration options// The settings object provides many configuration options
    val parserSettings = new CsvParserSettings

    //You can configure the parser to automatically detect what line separator sequence is in the input
    parserSettings.setLineSeparatorDetectionEnabled(true)

    // A RowListProcessor stores each parsed row in a List.
    val rowProcessor = new RowListProcessor

    // You can configure the parser to use a RowProcessor to process the values of each parsed row.
    // You will find more RowProcessors in the 'com.univocity.parsers.common.processor' package, but you can also create your own.
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

  def filterData(headers: Array[String], rows: List[Array[String]])(nfaItemType: String)(baseDate: LocalDate): (Array[Double], Array[Double]) = {
    val includedHeadersIdx = INCLUDED_HEADERS.map(headers.indexOf(_))
    val includedRows =
    rows.map(row =>
    // filter unnecessary data
      row.zipWithIndex.filter
      {
        case (_, i) => includedHeadersIdx.contains(i)
      }.unzip._1.filter(_ != null)
    // filter missing data, selected type, and Form 3's
    ).filter(_.length > 3).filter(_.contains(nfaItemType)).filterNot(_.contains("Form 3 To Dealer"))
    // filter unnecessary data once more
    .map(row =>
      row.zipWithIndex.filterNot
      {
        case (_, i) => i == 0 || i == 1
      }.unzip._1
    //convert dates to DateTime type, filter invalids
    ).map(_.map(s => Try(DateTime.parse(s))).filter(_.isSuccess)
    // convert dates to timestamp around given base date, filter dates before epoch
    .map(d =>Days.daysBetween(baseDate, d.get.toLocalDate).getDays.toDouble).filter(_ > 0)).filter(_.length > 1)
    // filter ridiculous outliers
    .filterNot(e =>
      {
        e(1) - e(0) < 14 || e(1) - e(0) > 1000 // fewer than two weeks, or over 1000 days
      }
    )
    .toArray

    // transpose result
    (includedRows.map{_(0)}, includedRows.map{_(1)})
  }

  val parser = new scopt.OptionParser[Config]("LinearRegression.scala") {
    head("NFATracker Linear Regression Analysis", "0.2")

    opt[String]('b', "baseDate").required.action( (x, c) =>
      c.copy(baseDate = x) ).text("Date around which to normalize data (i.e. earliest data used in prediction). Format: yyyy-MM-DD")

    opt[String]('d', "date").required.action( (x, c) =>
      c.copy(date = x) ).text("Date check was cashed by the NFA and for which the prediction is made. Format: yyyy-MM-DD")

    opt[String]('t', "nfa-item-type").required.action( (x, c) =>
      c.copy(itemType = x) ).text("Type of NFA item on which to resrict data.")

    opt[Unit]("plot-regression").hidden().action( (_, c) =>
      c.copy(plot = true) ).text("Plot a linear regression of data normalized around BASEDATE.")

    opt[Unit]('v', "verbose").action( (_, c) =>
      c.copy(verbose = true) ).text("Print verbose information during execution.")

    help("help").text("prints this usage text")
  }

  // parser.parse returns Option[C]
  parser.parse(args, Config()) match {
    case Some(config) =>
      // filter data
      val baseDate = DateTime.parse(config.baseDate).toLocalDate
      val (x, y) = (filterData _).tupled(generateData(NFATRACKER_URL))(config.itemType)(baseDate)

      // add constant to explanatory variables, and create model
      val model = ols(x.map(Array(1, _)),y)
      if (config.verbose) println(model)

      // predict approval date
      val date = Days.daysBetween(baseDate, DateTime.parse(config.date).toLocalDate).getDays.toDouble
      val prediction = model.predict(Array(1, date))

      // plot data, fit and prediction
      if (config.plot) {
        val plot = smile.plot.plot(Array(x, y).transpose, '@', Color.BLUE)
        val line = Array(Array(0.0, model.predict(Array(1, 0.0))), Array(date, prediction))
        plot.canvas.point('@', Color.RED, date, prediction)
        plot.canvas.line(line, Color.RED)
      }

      // print prediction
      val predictionDate = baseDate.plusDays(prediction.floor.toInt)
      println(predictionDate)
    case None =>
    // arguments are bad, error message will have been displayed
  }
}