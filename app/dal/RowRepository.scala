package dal

import javax.inject.{Inject, Singleton}

import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import models.Row
import org.joda.time.DateTime

import scala.concurrent.{ExecutionContext, Future}

/**
 * A repository for people.
 *
 * @param dbConfigProvider The Play db config provider. Play will inject this for you.
 */
@Singleton
class RowRepository @Inject() (dbConfigProvider: DatabaseConfigProvider)
                           (implicit ec: ExecutionContext) {
  // We want the JdbcProfile for this provider
  private val dbConfig = dbConfigProvider.get[JdbcProfile]

  // These imports are important, the first one brings db into scope, which will let you do the actual db operations.
  // The second one brings the Slick DSL into scope, which lets you define the table and other queries.
  import dbConfig._
  import profile.api._

  /**
   * Here we define the table. It will have "NFAItem", "FormType",
    * "Approved", "CheckCashed" columns.
   */
  private class RowTable(tag: Tag) extends Table[Row](tag, "rows") {

    /** The ID column, which is the primary key, and auto incremented */
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    /** The name column */
    def nfaItem = column[String]("NFAItem")

    /** The name column */
    def formType = column[String]("FormType")

    /** The name column */
    def approvedDate = column[Long]("Approved")

    /** The name column */
    def checkCashedDate = column[Long]("CheckCashed")

    /**
     * This is the tables default "projection".
     *
     * It defines how the columns are converted to and from the Row object.
     *
     * In this case, we are simply passing the id, name and page parameters to the Row case classes
     * apply and unapply methods.
     */
    def * = (id, nfaItem, formType, approvedDate, checkCashedDate) <> ((Row.apply _)
      .tupled, Row.unapply)
  }

  /**
   * The starting point for all queries on the rows table.
   */
  private val rows = TableQuery[RowTable]
  private val MAX_DAYS: Long = (8.64 * Math.pow(10, 10)).asInstanceOf[Long]
  /**
    * Curried filter function.
    * @param date Date after which the data is filtered.
    * @param item Item type on which to filter. Can be left unspecified.
    * @param approved Row approved date.
    * @param cashed Row check cashed date.
    * @param nfaItem Row item type.
    * @return If a row should be included.
    */
  private def filter(date: Long, item: Option[String])
                    (approved: Rep[Long], cashed: Rep[Long],
                     nfaItem: Rep[String]) : Rep[Boolean] = {
    val isWithinDateRange = approved >= date && cashed >= date && (approved - cashed) < MAX_DAYS
    item match {
      case Some(i) => isWithinDateRange && nfaItem === i
      case None => isWithinDateRange
    }
  }

  /**
   * Create a row with the given attributes.
   *
   * This is an asynchronous operation, it will return a future of the created Row, which can be used to obtain the
   * id for that Row.
   */
  def create(nfaItem: String, formType: String, checkCashedDate: DateTime,
             approvedDate: DateTime): Future[Row] = db.run {
    // We create a projection of just the name and age columns, since we're not inserting a value for the id column
    (rows.map(r => (r.nfaItem, r.formType, r.checkCashedDate, r.approvedDate))
      // Now define it to return the id, because we want to know what id was generated for the Row
      returning rows.map(_.id)
      // And we define a transformation for the returned value, which combines our original parameters with the
      // returned id
      into ((row, id) => Row(id, row._1, row._2, row._3, row._4))
    // And finally, insert the Row into the database
    ) += (nfaItem, formType, checkCashedDate.getMillis, approvedDate.getMillis)
  }

  /**
   * List all the people in the database.
   */
  def list(): Future[Seq[Row]] = db.run {
    rows.result
  }

  /**
    * Number of entries in the rows table.
    */
  def length(): Future[Int] = db.run {
    rows.length.result
  }

  /**
    * Update or insert given data into database.
    * @param id uid of row.
    * @param nfaItem Item type.
    * @param formType Form type.
    * @param approvedDate Date of approval.
    * @param checkCashedDate Date check was cashed.
    * @return
    */
  def update(id: Long, nfaItem:String, formType:String, checkCashedDate:DateTime, approvedDate:DateTime) = db.run {
    (rows returning rows).insertOrUpdate(Row(id, nfaItem, formType, approvedDate.getMillis, checkCashedDate.getMillis))
  }

  /**
    * Lists all the rows that match the given criteria.
    * @param baseDate Date after which the data is filtered.
    * @param nfaItem Item type on which to filter.
    * @return Rows given filter applied.
    */
  def listWithFilters(baseDate: String, nfaItem: Option[String]): Future[Seq[Row]] = db.run {
    val limit = DateTime.parse(baseDate).getMillis
    val isGraphed : (Rep[Long], Rep[Long], Rep[String]) => Rep[Boolean] // partially applied filter function
      = filter(limit, nfaItem)(_: Rep[Long], _: Rep[Long], _: Rep[String])

    ( for( c <- rows; if isGraphed(c.approvedDate, c.checkCashedDate, c.nfaItem)) yield c ).result
  }
}
