package com.owaspdemo.a05_injection;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * k8s-lab ONLY. Gated behind the "k8slab" Spring profile so it is never active
 * in the normal OWASP demo run.
 *
 * <p>This endpoint reproduces the same string-concatenation SQL injection bug as
 * {@link VulnerableProductController}, but executes the statement over a raw JDBC
 * {@link Statement} that permits stacked (multi-statement) queries and returns
 * every result set it produces. That makes the classic PostgreSQL
 * {@code COPY ... FROM PROGRAM} remote-code-execution technique observable through
 * the HTTP response, which is the first hop of the lateral-movement lab.
 *
 * <p>RCE here is only possible because the lab deliberately connects to PostgreSQL
 * as a superuser (see k8s-lab/manifests/10-postgres.yaml). The defensive lesson is
 * in k8s-lab/README.md: use parameterized queries AND a least-privilege DB role.
 */
@RestController
@RequestMapping("/api/v1/lab/products")
@Profile("k8slab")
@Tag(name = "k8s-lab - SQLi to RCE", description = "LAB ONLY: stacked-query SQL injection sink used to demonstrate PostgreSQL COPY ... FROM PROGRAM RCE")
public class LabRceController {

    private final DataSource dataSource;

    public LabRceController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping
    @Operation(
            summary = "Search products (stacked-query SQL injectable)",
            description = "LAB ONLY. Try the classic PostgreSQL RCE payload (URL-encode it):\n"
                    + "'; CREATE TABLE IF NOT EXISTS lab_out(line text); COPY lab_out FROM PROGRAM 'id'; SELECT line FROM lab_out; --")
    public Map<String, Object> search(
            @Parameter(description = "Search term", example = "' OR '1'='1")
            @RequestParam(defaultValue = "") String search) {
        // BAD: string concatenation straight into SQL — classic injection.
        // Executed via a raw JDBC Statement so stacked statements run and every
        // result set is returned to the caller.
        String sql = "SELECT name, description, price FROM product WHERE name LIKE '%" + search + "%'";

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("executedSql", sql);
        List<List<Map<String, Object>>> resultSets = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            boolean hasResultSet = stmt.execute(sql);
            while (true) {
                if (hasResultSet) {
                    try (ResultSet rs = stmt.getResultSet()) {
                        resultSets.add(readRows(rs));
                    }
                } else if (stmt.getUpdateCount() == -1) {
                    break; // no more results
                }
                hasResultSet = stmt.getMoreResults();
                if (!hasResultSet && stmt.getUpdateCount() == -1) {
                    break;
                }
            }
            response.put("resultSets", resultSets);
        } catch (SQLException e) {
            response.put("error", e.getMessage());
        }
        return response;
    }

    private List<Map<String, Object>> readRows(ResultSet rs) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        ResultSetMetaData md = rs.getMetaData();
        int cols = md.getColumnCount();
        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= cols; i++) {
                row.put(md.getColumnLabel(i), rs.getObject(i));
            }
            rows.add(row);
        }
        return rows;
    }
}
