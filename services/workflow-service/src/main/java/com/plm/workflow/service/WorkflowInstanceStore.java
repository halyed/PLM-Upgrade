package com.plm.workflow.service;

import com.plm.workflow.dto.PendingTask;
import com.plm.workflow.dto.WorkflowInstance;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store for workflow instances and pending tasks.
 */
@Component
public class WorkflowInstanceStore {

    private final Map<String, WorkflowInstance> byKey      = new ConcurrentHashMap<>();
    private final Map<Long, List<String>> byRevision       = new ConcurrentHashMap<>();
    private final Map<Long, PendingTask> pendingByJobKey   = new ConcurrentHashMap<>();

    // ── Workflow instances ────────────────────────────────────────────────────

    public void save(WorkflowInstance instance) {
        byKey.put(instance.getProcessInstanceKey(), instance);
        byRevision.computeIfAbsent(instance.getRevisionId(), k -> new ArrayList<>())
                  .add(instance.getProcessInstanceKey());
    }

    public List<WorkflowInstance> findByRevision(Long revisionId) {
        return byRevision.getOrDefault(revisionId, List.of()).stream().map(byKey::get).toList();
    }

    public List<WorkflowInstance> findAll() {
        return new ArrayList<>(byKey.values());
    }

    public void updateStatus(String processInstanceKey, String status, String currentStep) {
        WorkflowInstance inst = byKey.get(processInstanceKey);
        if (inst != null) {
            inst.setStatus(status);
            inst.setCurrentStep(currentStep);
        }
    }

    // ── Pending tasks ─────────────────────────────────────────────────────────

    public void savePendingTask(PendingTask task) {
        pendingByJobKey.put(task.getJobKey(), task);
    }

    public Optional<PendingTask> findPendingTask(long jobKey) {
        return Optional.ofNullable(pendingByJobKey.get(jobKey));
    }

    public List<PendingTask> findAllPendingTasks() {
        return new ArrayList<>(pendingByJobKey.values());
    }

    public void removePendingTask(long jobKey) {
        pendingByJobKey.remove(jobKey);
    }
}
