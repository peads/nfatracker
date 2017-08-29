package controllers

import javax.inject._

import com.github.nscala_time.time.Imports.DateTime
import dal._
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.i18n._
import play.api.libs.json.Json
import play.api.mvc._
import utils.NfaTrackerDataUpdater

import scala.concurrent.{ExecutionContext, Future}

class PersonController @Inject()(repo: RowRepository,
                                  cc: ControllerComponents
                                )(implicit ec: ExecutionContext)
  extends AbstractController(cc) with I18nSupport {

  private val NFA_TRACKER_URL = "http://www.nfatracker" +
    ".com/wp-content/themes/smartsolutions/inc/export/"

  /**
   * The index action.
   */
  def index = Action.async { implicit request =>
    repo.list().map(req => Ok(views.html.index(req)))
  }

  case class CreateRowForm(nfaItem:String, formType:String,approvedDate:String,
                              checkCashedDate:String)
  val rowForm: Form[CreateRowForm] = Form {
    mapping(
      "NFAItem" -> nonEmptyText,
      "FormType" -> nonEmptyText,
      "Approved" -> nonEmptyText,
      "CheckCashed" -> nonEmptyText
    )(CreateRowForm.apply)(CreateRowForm.unapply)
  }

  def updateRows = Action { implicit request =>
    (NfaTrackerDataUpdater.filterData _).tupled(NfaTrackerDataUpdater.generateData
    (NFA_TRACKER_URL)).foreach((repo.create _).tupled(_))
    Redirect(routes.PersonController.index)
  }

  /**
   * The add person action.
   *
   * This is asynchronous, since we're invoking the asynchronous methods on PersonRepository.
   */
//  def addPerson = Action.async { implicit request =>
//    // Bind the form first, then fold the result, passing a function to handle errors, and a function to handle succes.
//    rowForm.bindFromRequest.fold(
//      // The error function. We return the index page with the error form, which will render the errors.
//      // We also wrap the result in a successful future, since this action is synchronous, but we're required to return
//      // a future because the person creation function returns a future.
//      errorForm => {
//        Future.successful(Ok(views.html.index(errorForm)))
//      },
//      // There were no errors in the from, so create the person.
//      r => {
//        repo.create(r.nfaItem, r.formType, DateTime.parse(r.approvedDate), DateTime.parse(r
//          .checkCashedDate)).map { _ =>
//          // If successful, we simply redirect to the index page.
//          Redirect(routes.PersonController.index)
//        }
//      }
//    )
//  }

  /**
   * A REST endpoint that gets all the people as JSON.
   */
  def getPersons = Action.async { implicit request =>
    repo.list().map { people =>
      Ok(Json.toJson(people))
    }
  }
}
