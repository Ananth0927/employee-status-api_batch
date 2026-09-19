package com.example.employeebatch.partition;

import org.springframework.batch.core.partition.Partitioner;
import org.springframework.batch.infrastructure.item.ExecutionContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class EmployeeCsvPartitioner implements Partitioner {

    private final Path csvFile;
    private final int gridSize;

    public EmployeeCsvPartitioner(Path csvFile, int gridSize) {
        this.csvFile = csvFile;
        this.gridSize = gridSize;
    }

    @Override
    public Map<String, ExecutionContext> partition(int ignoredGridSize) {
        long dataRows;
        try {
            dataRows = Math.max(0, Files.lines(csvFile).count() - 1);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot count CSV rows: " + csvFile, e);
        }

        int partitions = (int) Math.min(Math.max(1, gridSize), Math.max(1, dataRows));
        long rowsPerPartition = (long) Math.ceil((double) dataRows / partitions);

        Map<String, ExecutionContext> result = new LinkedHashMap<>();
        for (int i = 0; i < partitions; i++) {
            long start = i * rowsPerPartition;
            long count = Math.min(rowsPerPartition, dataRows - start);
            if (count <= 0) {
                break;
            }

            ExecutionContext context = new ExecutionContext();
            context.putLong("startLine", start);
            context.putLong("itemCount", count);
            result.put("partition-" + i, context);
        }
        return result;
    }
}
