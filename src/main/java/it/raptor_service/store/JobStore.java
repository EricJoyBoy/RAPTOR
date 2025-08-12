package it.raptor_service.store;

import it.raptor_service.model.RaptorResult;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JobStore {

    private final Map<String, Job> jobs = new ConcurrentHashMap<>();

    public void storeJob(String jobId, CompletableFuture<RaptorResult> future) {
        jobs.put(jobId, new Job(future));
    }

    public Job getJob(String jobId) {
        return jobs.get(jobId);
    }

    public static class Job {
        private final CompletableFuture<RaptorResult> future;

        public Job(CompletableFuture<RaptorResult> future) {
            this.future = future;
        }

        public JobStatus getStatus() {
            if (future.isDone()) {
                if (future.isCompletedExceptionally()) {
                    return new JobStatus("FAILED", null);
                } else {
                    try {
                        return new JobStatus("COMPLETED", future.get());
                    } catch (Exception e) {
                        return new JobStatus("FAILED", null);
                    }
                }
            } else {
                return new JobStatus("IN_PROGRESS", null);
            }
        }
    }

    public record JobStatus(String status, RaptorResult result) {
    }
}
