package com.example.employeebatch.writer;

import com.example.employeebatch.domain.EmployeeStatusUpdate;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.jdbc.core.JdbcTemplate;

public class EmployeeStatusWriter implements ItemWriter<EmployeeStatusUpdate> {

    private final JdbcTemplate jdbcTemplate;

    public EmployeeStatusWriter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void write(Chunk<? extends EmployeeStatusUpdate> chunk) {
        jdbcTemplate.batchUpdate(
                "update employees.employee set status = ?, status_date = ?, updated_at = current_timestamp where employee_id = ?",
                chunk.getItems(),
                chunk.size(),
                (ps, item) -> {
                    ps.setString(1, item.status());
                    ps.setObject(2, item.statusDate());
                    ps.setLong(3, item.employeeId());
                });
    }
}
