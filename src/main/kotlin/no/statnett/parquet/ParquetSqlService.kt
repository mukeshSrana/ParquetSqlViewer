package no.statnett.parquet

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import java.sql.DriverManager

@Service(Service.Level.APP)
class ParquetSqlService : Disposable {
    private val connection = DriverManager.getConnection("jdbc:duckdb:")

    companion object {
        init {
            Class.forName("org.duckdb.DuckDBDriver")
        }
        fun getInstance(): ParquetSqlService = service()
    }

    fun runQuery(query: String): Pair<List<String>, List<List<Any?>>> {
        require(query.isNotBlank()) { "Query cannot be blank" }

        connection.createStatement().use { stmt ->
            stmt.queryTimeout = 60
            stmt.executeQuery(query).use { rs ->
                val meta = rs.metaData
                val columns = (1..meta.columnCount).map { meta.getColumnName(it) }
                val rows = mutableListOf<List<Any?>>()
                while (rs.next()) {
                    rows.add((1..meta.columnCount).map { rs.getObject(it) })
                }
                return columns to rows
            }
        }
    }

    override fun dispose() {
        connection.close()
    }
}
