CREATE TABLE IF NOT EXISTS employees (
    employee_id BIGINT PRIMARY KEY,
    employee_name VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL,
    status_date DATE,
    updated_at TIMESTAMP NULL
);
