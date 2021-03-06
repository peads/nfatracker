package controllers

import javax.inject._

import dal.RowRepository
import org.joda.time.DateTime
import play.api.Logger
import play.api.i18n._
import play.api.libs.json.Json
import play.api.mvc._
import utils.{Regression, UpdateAction}

import scala.concurrent.{Await, ExecutionContext}

class RowController @Inject()(updateAction: UpdateAction, repo: RowRepository,
                              cc: ControllerComponents)
                             (implicit ec: ExecutionContext) extends AbstractController(cc)
  with I18nSupport with Regression {
  /**
    * Partially applied function allowing mixin to access injected database
    * reference.
    */
  private val PREDICT: (DateTime, DateTime, Option[String]) => List[(String, Long, Long, String)]
  = predict(repo)(_: DateTime, _: DateTime, _: Option[String])

  /**
    * The list action.
    */
  def list = Action.async { implicit request => repo.list().map(req => Ok(views.html.list(req))) }

  /**
    * Update database with new transfers from NFATracker.
    * Uses custom UpdateAction as ACL only allowing restricted access based on IP.
    */
  def updateRows = updateAction { implicit request =>
    val initialTableSize = Await.result(repo.length(), DURATION)

    (filterData _).tupled(generateData(NFA_TRACKER_URL)).zipWithIndex.foreach {
      case ((nfaItem, formType, checkCashedDate, approvedDate), id) =>
        Await.result(
          repo.update(id, nfaItem, formType, checkCashedDate, approvedDate)
          , DURATION) match {
          case Some(r) => Logger.debug("Inserted " + r.toString)
          case None => Logger.debug("Updated " + id)
        }
    }

    val finalTableSize = Await.result(repo.length(), DURATION)
    val updateSize = finalTableSize - initialTableSize
    Logger.info(s"Number updated in database: $updateSize")

    Redirect(routes.RowController.index)
  }

  /**
    * A REST endpoint that gets all the transfers as JSON.
    */
  def getJson = Action.async { implicit request => repo.list().map { rows => Ok(Json.toJson(rows)) } }

  /**
    * A REST endpoint that gets all the filtered transfers as JSON.
    */
  def getFilteredJson(baseDate: String, nfaType: String) = Action.async { implicit request =>
    repo.listWithFilters(baseDate, Option(nfaType)).map { rows => Ok(Json.toJson(rows)) }
  }

  /**
    * A REST endpoint that gets the prediction given the base date, check cashed date and item type.
    */
  def getPrediction(date: String, baseDate: String, nfaType: String) = Action { implicit request =>
    Ok(Json.toJson(PREDICT(DateTime.parse(baseDate), DateTime.parse(date), Option(nfaType))))
  }

  /**
    * The index action.
    */
  def index = Action { implicit request => Ok(views.html.index()) }
}
