package com.example.employeebatch.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class EmployeeBatchScheduler {

    private static final Logger log = LoggerFactory.getLogger(EmployeeBatchScheduler.class);

    private final JobOperator jobOperator;
    private final Job employeeDailyStatusJob;

    public EmployeeBatchScheduler(JobOperator jobOperator, Job employeeDailyStatusJob) {
        this.jobOperator = jobOperator;
        this.employeeDailyStatusJob = employeeDailyStatusJob;
    }

    @Scheduled(cron = "${employee.batch.cron:0 0 1 * * *}", zone = "${employee.batch.zone:Asia/Kolkata}")
    public void runDaily() {
        JobParameters parameters = new JobParametersBuilder()
                .addLocalDate("employeeDate", LocalDate.now())
                .toJobParameters();
        try {
            var execution = jobOperator.start(employeeDailyStatusJob, parameters);
            log.info("Employee daily status job started. executionId={}, status={}",
                    execution.getId(), execution.getStatus());
        } catch (Exception e) {
            log.error("Employee daily status job could not be started", e);
        }
    }
}
