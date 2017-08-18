import com.github.nscala_time.time.Imports._

case class Config(baseDate: String = "", date: String = "", itemType: String = "", verbose: Boolean = false,
                  plot: Boolean = false)

object Main extends App with LinearRegression {

  val parser = new scopt.OptionParser[Config]("LinearRegression") {
    head("NFATracker Linear Regression Analysis", "0.2")

    opt[String]('b', "baseDate").required.action((x, c) =>
      c.copy(baseDate = x)).text("Date around which to normalize data (i.e. earliest data used in prediction). " +
      "Format: yyyy-MM-DD")

    opt[String]('d', "date").required.action((x, c) =>
      c.copy(date = x)).text("Date check was cashed by the NFA and for which the prediction is made. Format: " +
      "yyyy-MM-DD")

    opt[String]('t', "nfa-item-type").required.action((x, c) =>
      c.copy(itemType = x)).text("Type of NFA item on which to resrict data.").
      validate(x =>
        if (List("Suppressor", "SBR", "SBS", "MG", "AOW").contains(x)) success
        else failure("Option --nfa-item-type must be \"Suppressor\", \"SBR\", \"SBS\", \"MG\", or \"AOW\""))

    opt[Unit]("plot-regression").hidden().action((_, c) =>
      c.copy(plot = true)).text("Plot a linear regression of data normalized around BASEDATE.")

    opt[Unit]('v', "verbose").action((_, c) =>
      c.copy(verbose = true)).text("Print verbose information during execution.")

    help("help").text("prints this usage text")
  }

  // parser.parse returns Option[C]
  parser.parse(args, Config()) match {
    case Some(config) =>
      val prediction = predict(config.baseDate, config.date, config.itemType, config.verbose, config.plot)

      // print prediction
      val predictionDate = DateTime.parse(config.baseDate).toLocalDate.plusDays(prediction.floor.toInt)
      println(predictionDate)
    case None =>
    // arguments are bad, error message will have been displayed
  }
}