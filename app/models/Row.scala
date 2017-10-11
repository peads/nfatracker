package models

import play.api.libs.json._

case class Row(id: Long, nfaItem: String, formType: String, approvedDate: Long, checkCashedDate: Long)

object Row { implicit val rowFormat = Json.format[Row] }
