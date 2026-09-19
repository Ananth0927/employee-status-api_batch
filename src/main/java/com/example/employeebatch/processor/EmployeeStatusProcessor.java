package com.example.employeebatch.processor;

import com.example.employeebatch.domain.EmployeeStatusUpdate;
import com.example.employeebatch.exception.EmployeeNotFoundException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.batch.infrastructure.item.ItemProcessor;

public class EmployeeStatusProcessor implements ItemProcessor<EmployeeStatusUpdate, EmployeeStatusUpdate> {

    private final JdbcTemplate jdbcTemplate;

    public EmployeeStatusProcessor(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public EmployeeStatusUpdate process(EmployeeStatusUpdate item) {
//        Integer count = jdbcTemplate.queryForObject(
//                "select count(*) from employees.employee where employee_id = ?",
//                Integer.class,
//                item.employeeId());
//
//        if (count == null || count == 0) {
//            throw new EmployeeNotFoundException("Employee not found: " + item.employeeId());
//        }

        return item;
    }
}
