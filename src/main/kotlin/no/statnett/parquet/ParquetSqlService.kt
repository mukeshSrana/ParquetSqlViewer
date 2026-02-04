package no.statnett.parquet

import java.sql.DriverManager

class ParquetSqlService {
    fun runQuery(query: String): Pair<List<String>, List<List<Any?>>> {
        // Ensure Driver is loaded
        Class.forName("org.duckdb.DuckDBDriver")

        return DriverManager.getConnection("jdbc:duckdb:").use { conn ->
            conn.createStatement().use { stmt ->
                stmt.executeQuery(query).use { rs ->
                    val meta = rs.metaData
                    val colCount = meta.columnCount
                    val columns = (1..colCount).map { meta.getColumnName(it) }

                    val data = mutableListOf<List<Any?>>()
                    while (rs.next()) {
                        val row = (1..colCount).map { rs.getObject(it) }
                        data.add(row)
                    }
                    Pair(columns, data)
                }
            }
        }
    }
}