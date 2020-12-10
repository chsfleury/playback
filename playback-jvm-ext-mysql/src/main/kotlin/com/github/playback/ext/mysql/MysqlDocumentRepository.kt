package com.github.playback.ext.mysql

import com.github.playback.api.*
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jooq.DSLContext
import org.jooq.JSON
import org.jooq.Record4
import org.jooq.SQLDialect
import org.jooq.impl.DSL.*
import org.jooq.impl.DefaultDSLContext
import org.jooq.impl.SQLDataType
import org.slf4j.LoggerFactory
import java.sql.Timestamp
import javax.sql.DataSource

class MysqlDocumentRepository(
  private val json: JsonMapper,
  private val url: String,
  private val user: String,
  private val pass: String
) : DocumentRepository {
  companion object {
    const val DATABASE_ERROR = "database error"

    private val log = LoggerFactory.getLogger(MysqlDocumentRepository::class.java)
    private val idField = field("id", SQLDataType.VARCHAR.length(255).nullable(false))
    private val dataField = field("data", SQLDataType.JSON.nullable(true))
    private val timestampField = field("timestamp", SQLDataType.TIMESTAMP.precision(3).nullable(false))
    private val patchField = field("patch", SQLDataType.BOOLEAN.nullable(false))
  }

  private val jooq = createJooq()

  override fun listCollections(): Result<WebError, List<String>> = try {
    val tables = jooq.meta().schemas.find { it.name == "playback" }?.tables?.map { it.name }
    if (tables != null) {
      success(tables)
    } else {
      failure(databaseError("database playback not found"))
    }
  } catch (e: Exception) {
    failure(databaseError(e))
  }

  override fun createCollection(collection: String): WebError? = try {
    createTables(collection)
    null
  } catch (e: Exception) {
    databaseError(e, collection)
  }

  override fun clearCollection(collection: String): WebError? = try {
    jooq.truncate(collection).execute()
    null
  } catch (e: Exception) {
    databaseError(e, collection)
  }

  override fun deleteCollection(collection: String): WebError? = try {
    jooq.dropTable(collection).execute()
    null
  } catch (e: Exception) {
    databaseError(e, collection)
  }

  override fun getLastCompleteDocumentTimestamp(collection: String, documentId: String, timestamp: Long?): Long? {
    return jooq.select(timestampField)
      .from(table(collection))
      .let { req ->
        if (timestamp != null) {
          req.where(idField.eq(documentId), timestampField.lessOrEqual(Timestamp(timestamp)), patchField.isFalse)
        } else {
          req.where(idField.eq(documentId), patchField.isFalse)
        }
      }
      .orderBy(timestampField.desc())
      .limit(1)
      .fetchOne()
      ?.value1()?.time
  }

  override fun getDocument(collection: String, documentId: String, timestamp: Long?): Result<WebError, Document> = try {
    val document = jooq
      .select(idField, timestampField, dataField, patchField)
      .from(table(collection))
      .let { req ->
        if (timestamp != null) {
          req.where(idField.eq(documentId), timestampField.lessOrEqual(Timestamp(timestamp)))
        } else {
          req.where(idField.eq(documentId))
        }
      }
      .orderBy(timestampField.desc())
      .limit(1)
      .fetchOne()
      ?.let(this::convert)

    if (document == null) {
      failure(WebError(404, "document not found"))
    } else {
      success(document)
    }
  } catch (e: Exception) {
    failure(databaseError(e, collection))
  }

  override fun saveDocument(collection: String, document: Document): WebError? = try {
    val jsonData = document.fields
      .takeIf { it.isNotEmpty() }
      ?.let { JSON.json(json(it)) }

    jooq.insertInto(table(collection), idField, timestampField, dataField, patchField)
      .values(document.id, Timestamp(document.timestamp), jsonData, document.patch)
      .execute() == 0

    null
  } catch (e: Exception) {
    databaseError(e, collection)
  }

  override fun deleteDocument(collection: String, documentId: String): WebError? = try {
    val count = jooq.selectCount().from(table(collection)).where(idField.eq(documentId)).fetchOne()
    if (count == null || count.value1() == 0) {
      WebError(404, "document not found")
    } else {
      jooq.delete(table(collection)).where(idField.eq(documentId)).execute()
      null
    }
  } catch (e: Exception) {
    databaseError(e, collection)
  }

  override fun getLastUpdates(collection: String, documentId: String, n: Int): Result<WebError, List<Document>> = try {
    val result = jooq
      .select(idField, timestampField, dataField, patchField)
      .from(table(collection))
      .where(idField.eq(documentId))
      .orderBy(timestampField.desc())
      .limit(n)
      .fetch()
      .map(this::convert)

    success(result)
  } catch (e: Exception) {
    failure(databaseError(e, collection))
  }

  private fun convert(record: Record4<String, Timestamp, JSON, Boolean>): Document = Document(
    record.value1(),
    record.value2().time,
    record.value4(),
    record.value3()?.data()?.let { json(it) } ?: mapOf()
  )

  override fun getUpdates(
    collection: String,
    documentId: String,
    timestampStart: Long?,
    timestampEnd: Long?
  ): Result<WebError, List<Document>> = try {
    var req = jooq
      .select(idField, timestampField, dataField, patchField)
      .from(table(collection))
      .where(idField.eq(documentId))

    if (timestampStart != null) {
      req = req.and(timestampField.greaterOrEqual(Timestamp(timestampStart)))
    }

    if (timestampEnd != null) {
      req = req.and(timestampField.lessOrEqual(Timestamp(timestampEnd)))
    }

    val documents = req
      .orderBy(timestampField.asc())
      .fetch()
      .map(this::convert)

    success(documents)
  } catch (e: Exception) {
    failure(databaseError(e, collection))
  }

  private fun createTables(name: String): Int = jooq
    .createTable(name)
    .columns(idField, dataField, timestampField, patchField)
    .constraint(primaryKey(idField, timestampField))
    .execute()

  private fun createJooq(): DSLContext = DefaultDSLContext(createDatasource(), SQLDialect.MYSQL)

  private fun createDatasource(): DataSource = HikariDataSource(
    HikariConfig().apply {
      username = user
      password = pass
      jdbcUrl = url
      driverClassName = "com.mysql.cj.jdbc.Driver"
      connectionTimeout = 30000L
    }
  )

  private fun databaseError(e: Exception, collection: String? = null): WebError {
    log.error(DATABASE_ERROR, e)
    return when {
      endsWith(e, "Table '$collection' already exists") -> WebError(409, "Collection '$collection' already exists")
      endsWith(e, "Unknown table 'playback.$collection'") -> WebError(404, "Collection '$collection' does not exists")
      else -> WebError(500, DATABASE_ERROR, e.message)
    }
  }

  private fun databaseError(details: String) = WebError(500, DATABASE_ERROR, details)

  private fun endsWith(e: Exception, contentEnd: String): Boolean = e.message?.endsWith(contentEnd) ?: false

}
