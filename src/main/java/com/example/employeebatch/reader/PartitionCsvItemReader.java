package com.example.employeebatch.reader;

import com.example.employeebatch.domain.EmployeeStatusUpdate;
import com.example.employeebatch.exception.CsvDataException;
import org.jspecify.annotations.NonNull;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

public class PartitionCsvItemReader implements ItemStreamReader<EmployeeStatusUpdate> {

    private static final String CURRENT_ITEM_KEY = "current.item";

    private final Path file;
    private final long startLine;
    private final long itemCount;

    private BufferedReader reader;
    private long currentItem;

    public PartitionCsvItemReader(Path file, long startLine, long itemCount) {
        this.file = file;
        this.startLine = startLine;
        this.itemCount = itemCount;
    }

    @Override
    public void open(ExecutionContext executionContext) {
        if (reader != null) {
            return;
        }
        try {
            reader = Files.newBufferedReader(file);
            reader.readLine(); // skip header

            long restartItem = executionContext.getLong(CURRENT_ITEM_KEY, 0L);
            long recordsToSkip = startLine + restartItem;

            for (long i = 0; i < recordsToSkip; i++) {
                if (reader.readLine() == null) {
                    break;
                }
            }

            currentItem = restartItem;
        } catch (IOException e) {
            throw new CsvDataException("Cannot open CSV: " + file + ": " + e.getMessage());
        }
    }

    @Override
    public EmployeeStatusUpdate read() {
        if (reader == null) {
            throw new IllegalStateException("PartitionCsvItemReader has not been opened");
        }

        if (currentItem >= itemCount) {
            return null;
        }

        try {
            String line = reader.readLine();
            if (line == null) {
                return null;
            }

            long csvDataLine = startLine + currentItem + 2;
            currentItem++;
            return parse(line, csvDataLine);
        } catch (IOException e) {
            throw new CsvDataException("CSV read failed at data record " + (startLine + currentItem));
        }
    }

    private EmployeeStatusUpdate parse(String line, long lineNumber) {
        String[] values = line.split(",", -1);
        if (values.length != 3) {
            throw new CsvDataException("Invalid CSV at line " + lineNumber + ": expected 3 columns");
        }

        try {
            Long employeeId = Long.valueOf(values[0].trim());
            String status = values[1].trim().toUpperCase();
            LocalDate statusDate = LocalDate.parse(values[2].trim());

            if (status.isBlank()) {
                throw new CsvDataException("Status is blank at line " + lineNumber);
            }

            return new EmployeeStatusUpdate(employeeId, status, statusDate);
        } catch (NumberFormatException | java.time.format.DateTimeParseException e) {
            throw new CsvDataException("Invalid data at CSV line " + lineNumber + ": " + line);
        }
    }

    @Override
    public void update(@NonNull ExecutionContext executionContext) {
        executionContext.putLong(CURRENT_ITEM_KEY, currentItem);
    }

    @Override
    public void close() {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException ignored) {
                // ignore
            } finally {
                reader = null;
            }
        }
    }
}