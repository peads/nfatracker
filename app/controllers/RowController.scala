package controllers

import java.awt.Color
import javax.inject._

import dal._
import org.joda.time.{DateTime, Days, LocalDate}
import play.api.i18n._
import play.api.libs.json.Json
import play.api.mvc._
import utils.NfaTrackerDataUpdater
import smile.regression._

import scala.concurrent.{Await, ExecutionContext}

class RowController @Inject()(repo: RowRepository,
                                  cc: ControllerComponents
                                )(implicit ec: ExecutionContext)
  extends AbstractController(cc) with I18nSupport {

  private val NFA_TRACKER_URL = "http://www.nfatracker" +
    ".com/wp-content/themes/smartsolutions/inc/export/"
  private val NFA_ITEM_TYPES = List("Suppressor", "SBR", "SBS", "MG", "AOW")

  /**
   * The index action.
   */
  def list = Action.async { implicit request =>
    repo.list().map(req => Ok(views.html.list(req)))
  }

  /**
    * Update database with new transfers from NFATracker.
    * TODO: Make it so this is only accessible from localhost.
    */
  def updateRows = Action { implicit request =>
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

  def index = Action { implicit request =>

    val baseDate = DateTime.parse("2016-01-01")
    val date = DateTime.parse("2016-09-06")
    val baseDateDouble = Days.daysBetween(repo.EPOCH, baseDate.toLocalDate)
      .getDays.toDouble
    val dateDouble = Days.daysBetween(baseDate, date).getDays.toDouble

    val dbResult = Await.result(repo.list(), scala.concurrent.duration.Duration
    (30, scala.concurrent.duration.SECONDS)).toArray
    val (x,y) = (dbResult.map(_.checkCashedDate.toDouble - baseDateDouble), dbResult.map(_
      .approvedDate.toDouble - baseDateDouble))

    // add constant to explanatory variables, and create model
    val model = ols(x.map(Array(1, _)),y)
    val prediction = model.predict(Array(1, dateDouble))

    Ok(views.html.index(prediction))
  }
}
