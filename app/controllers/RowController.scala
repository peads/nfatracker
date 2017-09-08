package controllers

import javax.inject._
import dal.RowRepository
import org.joda.time.{DateTime, Days}
import play.api.Logger
import play.api.i18n._
import play.api.libs.json.Json
import play.api.mvc._
import utils.{LinearRegression, UpdateAction}

import scala.concurrent.{Await, ExecutionContext}

class RowController @Inject()(updateAction: UpdateAction, repo: RowRepository,
                              cc: ControllerComponents)
                    (implicit ec: ExecutionContext) extends AbstractController(cc)
                    with I18nSupport with LinearRegression {
  /**
   * The list action.
   */
  def list = Action.async { implicit request =>
    repo.list().map(req => Ok(views.html.list(req)))
  }

  /**
    * Update database with new transfers from NFATracker.
    * Uses custom UpdateAction as ACL only allowing localhost access.
    */
  def updateRows = updateAction { implicit request =>
    val initialTableSize = Await.result(repo.length(), DURATION)

    (filterData _).tupled(generateData
    (NFA_TRACKER_URL)).drop(initialTableSize).foreach((repo.create _).tupled(_))

    val finalTableSize = Await.result(repo.length(), DURATION)
    val updateSize = finalTableSize - initialTableSize

    Logger.info(s"Database updated with $updateSize new entries.")

    Redirect(routes.RowController.index)
  }

  /**
   * A REST endpoint that gets all the transfers as JSON.
   */
  def getJson = Action.async { implicit request =>
    repo.list().map { rows =>
      Ok(Json.toJson(rows))
    }
  }

  /**
    * Partially applied function allowing mixin to access injected database
    * reference.
    */
  private val predict: (DateTime, DateTime, String) => String = predict(repo)(_:
    DateTime, _: DateTime, _: String)
  /**
    * The submit action.
    */
  def handleDateSubmit = Action { implicit request => {
      val body = request.body.asFormUrlEncoded
      val checkCashedString = body.get("checkCashed").mkString
      val baseDateString = body.get("base").mkString
      val nfaItemType = body.get("type").mkString
      Ok(views.html.index(predict(DateTime.parse(baseDateString), DateTime
        .parse(checkCashedString), nfaItemType)))
    }
  }
  /**
    * The index action.
    */
  def index = Action { implicit request =>
    Ok(views.html.index(""))
  }
}
