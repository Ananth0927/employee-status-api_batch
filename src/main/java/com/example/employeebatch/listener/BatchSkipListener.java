package com.example.employeebatch.listener;

import com.example.employeebatch.domain.EmployeeStatusUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.listener.SkipListener;

public class BatchSkipListener implements SkipListener<EmployeeStatusUpdate, EmployeeStatusUpdate> {

    private static final Logger log = LoggerFactory.getLogger(BatchSkipListener.class);

    @Override
    public void onSkipInRead(Throwable t) {
        log.error("Skipped CSV record while reading: {}", t.getMessage());
    }

    @Override
    public void onSkipInProcess(EmployeeStatusUpdate item, Throwable t) {
        log.error("Skipped employee {} during processing: {}", item.employeeId(), t.getMessage());
    }

    @Override
    public void onSkipInWrite(EmployeeStatusUpdate item, Throwable t) {
        log.error("Skipped employee {} during writing: {}", item.employeeId(), t.getMessage());
    }
}
