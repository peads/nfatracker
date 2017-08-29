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
  import org.joda.time.{Days, LocalDate}

  private val EPOCH = new LocalDate(1970, 1, 1)

  //Set to Epoch time

  /**
   * Here we define the table. It will have "NFAItem", "FormType",
    * "Approved", "CheckCashed" columns.
   */
  private class PeopleTable(tag: Tag) extends Table[Row](tag, "rows") {

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
   * The starting point for all queries on the people table.
   */
  private val rows = TableQuery[PeopleTable]

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
    ) += (nfaItem, formType, Days.daysBetween(EPOCH, checkCashedDate.toLocalDate).getDays
      .toLong, Days.daysBetween(EPOCH, approvedDate.toLocalDate).getDays.toLong)
  }

  /**
   * List all the people in the database.
   */
  def list(): Future[Seq[Row]] = db.run {
    rows.result
  }
}
