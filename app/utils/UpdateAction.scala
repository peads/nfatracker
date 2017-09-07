package utils

import javax.inject.Inject
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionBuilderImpl, BodyParsers, Request, Result}
import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by peads on 9/7/17.
  */
class UpdateAction @Inject() (parser: BodyParsers.Default)(implicit ec:
ExecutionContext) extends ActionBuilderImpl(parser) {
  override def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = {
    val headers = request.headers
    val remoteAddress = headers.get("Remote-Address").mkString
    if (remoteAddress.contains("127.0.0.1"))
      block(request)
    else
      Future.successful(Redirect("/"))
  }
}
