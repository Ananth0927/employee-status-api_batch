package com.example.employeebatch.config;

import com.example.employeebatch.domain.EmployeeStatusUpdate;
import com.example.employeebatch.exception.CsvDataException;
import com.example.employeebatch.exception.EmployeeNotFoundException;
import com.example.employeebatch.listener.BatchSkipListener;
import com.example.employeebatch.partition.EmployeeCsvPartitioner;
import com.example.employeebatch.processor.EmployeeStatusProcessor;
import com.example.employeebatch.reader.PartitionCsvItemReader;
import com.example.employeebatch.writer.EmployeeStatusWriter;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.Partitioner;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.batch.infrastructure.item.ItemStreamReader;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.util.Set;

@Configuration
public class BatchConfig {

    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    public TaskExecutor employeeTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(0);
        executor.setThreadNamePrefix("employee-batch-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }

    @Bean
    public Job employeeDailyStatusJob(JobRepository jobRepository,
                                      @Qualifier("employeeManagerStep") Step managerStep) {
        return new JobBuilder("employeeDailyStatusJob", jobRepository)
                .start(managerStep)
                .build();
    }

    @Bean
    public Step employeeManagerStep(JobRepository jobRepository,
                                    @Qualifier("employeePartitionHandler") PartitionHandler partitionHandler,
                                    @Qualifier("employeePartitioner") Partitioner partitioner) {
        return new StepBuilder("employeeManagerStep", jobRepository)
                .partitioner("employeeWorkerStep", partitioner)
                .partitionHandler(partitionHandler)
                .build();
    }

    @Bean
    public PartitionHandler employeePartitionHandler(
            @Qualifier("employeeTaskExecutor") TaskExecutor taskExecutor,
            @Qualifier("employeeWorkerStep") Step workerStep) throws Exception {
        TaskExecutorPartitionHandler handler = new TaskExecutorPartitionHandler();
        handler.setTaskExecutor(taskExecutor);
        handler.setStep(workerStep);
        handler.setGridSize(4);
        handler.afterPropertiesSet();
        return handler;
    }

    @Bean
    public Partitioner employeePartitioner(
            @Value("${employee.batch.csv-file}") String csvFile,
            @Value("${employee.batch.grid-size:4}") int gridSize) {
        return new EmployeeCsvPartitioner(Path.of(csvFile), gridSize);
    }

//commented this line due to unexpected  runtime error
    @Bean
    public Step employeeWorkerStep(JobRepository jobRepository,
                                   PlatformTransactionManager transactionManager,
                                   @Qualifier("employeeReader") ItemStreamReader<EmployeeStatusUpdate> reader,                                   @Qualifier("employeeProcessor") ItemProcessor<EmployeeStatusUpdate, EmployeeStatusUpdate> processor,
                                   @Qualifier("employeeWriter") ItemWriter<EmployeeStatusUpdate> writer,
                                   BatchSkipListener skipListener) {
        return new StepBuilder("employeeWorkerStep", jobRepository)
                .<EmployeeStatusUpdate, EmployeeStatusUpdate>chunk(20)
                .transactionManager(transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
//                .faultTolerant()
//                .retryPolicy(retryPolicy())
//                .skipPolicy(skipPolicy())
                .skipListener(skipListener)
                .build();
    }

    @Bean
    @StepScope
    public PartitionCsvItemReader employeeReader(
            @Value("${employee.batch.csv-file}") String csvFile,
            @Value("#{stepExecutionContext['startLine']}") Long startLine,
            @Value("#{stepExecutionContext['itemCount']}") Long itemCount) {
        return new PartitionCsvItemReader(Path.of(csvFile), startLine, itemCount);
    }

    @Bean
    public ItemProcessor<EmployeeStatusUpdate, EmployeeStatusUpdate> employeeProcessor(JdbcTemplate jdbcTemplate) {
        return new EmployeeStatusProcessor(jdbcTemplate);
    }

    @Bean
    public ItemWriter<EmployeeStatusUpdate> employeeWriter(JdbcTemplate jdbcTemplate) {
        return new EmployeeStatusWriter(jdbcTemplate);
    }

    @Bean
    public BatchSkipListener batchSkipListener() {
        return new BatchSkipListener();
    }

    @Bean
    public RetryPolicy retryPolicy() {
        return RetryPolicy.builder()
                .maxRetries(3)
                .includes(Set.of(org.springframework.dao.DeadlockLoserDataAccessException.class,
                        org.springframework.dao.CannotAcquireLockException.class))
                .build();
    }

    @Bean
    public org.springframework.batch.core.step.skip.SkipPolicy skipPolicy() {
        return (throwable, skipCount) -> {
            if (skipCount >= 100) {
                return false;
            }
            return throwable instanceof CsvDataException
                    || throwable instanceof EmployeeNotFoundException;
        };
    }
}
