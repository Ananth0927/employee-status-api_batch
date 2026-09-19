# Employee Daily Status Spring Batch

Stack:
- Spring Boot 4.0.8
- Spring Batch 6.x (managed by Spring Boot 4.0.8)
- Java 25
- MySQL 8+
- Maven

## What this project demonstrates

1. Reads employee daily status from CSV.
2. Partitions the CSV into four ranges.
3. Runs partitions concurrently with a `ThreadPoolTaskExecutor`.
4. Processes each record and checks that the employee exists.
5. Updates the employee status in MySQL in chunks of 20.
6. Skips malformed CSV rows and employees not found, up to 100 skips.
7. Retries database deadlock/lock exceptions up to three retries.
8. Uses Spring Batch JDBC metadata tables for restartability and monitoring.
9. Uses Spring Batch 6's `JobOperator.start(Job, JobParameters)` rather than the deprecated `JobLauncher.run(...)` style.
10. Schedules the job every day at 1:00 AM Asia/Kolkata.

## 1. Create the database

```sql
CREATE DATABASE employee_batch;
USE employee_batch;
SOURCE src/main/resources/schema.sql;
SOURCE src/main/resources/db/sample-employees.sql;
```

Or execute the SQL files manually from MySQL Workbench.

Update `application.properties` with your MySQL password.

## 2. CSV format

```text
employeeId,status,statusDate
1001,ACTIVE,2026-08-29
1002,INACTIVE,2026-08-29
```

## 3. Run

```bash
mvn clean package
java -jar target/employee-status-batch-1.0.0.jar
```

The application starts but does not run the job immediately because:

```properties
spring.batch.job.enabled=false
```

The scheduler starts it every day at 1 AM.

For testing, temporarily change the cron to every minute:

```properties
employee.batch.cron=0 * * * * *
```

## 4. Verify

```sql
SELECT * FROM employees ORDER BY employee_id;

SELECT JOB_INSTANCE_ID, JOB_NAME FROM BATCH_JOB_INSTANCE;
SELECT * FROM BATCH_JOB_EXECUTION ORDER BY JOB_EXECUTION_ID DESC;
SELECT * FROM BATCH_STEP_EXECUTION ORDER BY STEP_EXECUTION_ID DESC;
```

## Important Spring Batch 6 note

Spring Batch 6 moved many item APIs to `org.springframework.batch.infrastructure.item.*`. This project intentionally uses those Batch 6 package names.

The project uses a local `TaskExecutorPartitionHandler`, so partitions run in multiple threads inside the same JVM. Each partition gets its own step-scoped CSV reader; stateful readers must not be shared between partition threads.
