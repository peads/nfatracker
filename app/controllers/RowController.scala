package controllers

import javax.inject._

import dal._
import org.joda.time.{DateTime, Days}
import play.api.i18n._
import play.api.libs.json.Json
import play.api.mvc._
import utils.{NfaTrackerDataUpdater, UpdateAction}
import smile.regression._

import scala.concurrent.{Await, ExecutionContext}

class RowController @Inject()(updateAction: UpdateAction, repo: RowRepository,
                              cc: ControllerComponents)
                    (implicit ec: ExecutionContext) extends AbstractController(cc)
                    with I18nSupport {

  private val NFA_TRACKER_URL = "http://www.nfatracker.com/wp-content/themes/smartsolutions/inc/export/"
  private val NFA_ITEM_TYPES = List("Suppressor", "SBR", "SBS", "MG", "AOW")

  /**
   * The index action.
   */
  def list = Action.async { implicit request =>
    repo.list().map(req => Ok(views.html.list(req)))
  }

  /**
    * Update database with new transfers from NFATracker.
    */
  def updateRows = updateAction { implicit request =>
    (NfaTrackerDataUpdater.filterData _).tupled(NfaTrackerDataUpdater.generateData
    (NFA_TRACKER_URL)).foreach((repo.create _).tupled(_))
    Redirect(routes.RowController.index)
  }

  /**
   * A REST endpoint that gets all the transfers as JSON.
   */
  def getJson = Action.async { implicit request =>
    repo.list().map { people =>
      Ok(Json.toJson(people))
    }
  }
  private def outOfRangeFilter(checkCashedDate: Double, approvedDate: Double)
  : Boolean = {
    val timeDiff = approvedDate - checkCashedDate
    approvedDate > 0 && checkCashedDate > 0 && timeDiff >= 14 && timeDiff < 1000
  }
  private def normalizedTimeStamp(baseDate: DateTime, date: DateTime) : Double =
    Days.daysBetween(baseDate, date).getDays.toDouble

  private def predict(baseDate: DateTime, date: DateTime, nfaType: String): String = {
    val dateDouble = normalizedTimeStamp(baseDate, date)

    val dbResult = Await.result(repo.list(), scala.concurrent.duration.Duration
    (30, scala.concurrent.duration.SECONDS)).filter(_.nfaItem.contains(nfaType))
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
  def handleDateSubmit = Action { implicit request => {
      val body = request.body.asFormUrlEncoded
      val checkCashedString = body.get("checkCashed").mkString
      val baseDateString = body.get("base").mkString
      val nfaItemType = body.get("type").mkString
      Ok(views.html.index(predict(DateTime.parse(baseDateString), DateTime
        .parse(checkCashedString), nfaItemType)))
    }
  }
  def index = Action { implicit request =>
    Ok(views.html.index(""))
  }
}
