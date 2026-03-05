package com.plm.workflow.service;

import com.plm.workflow.dto.WorkflowInstance;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store for workflow instances.
 * Keyed by processInstanceKey; also indexed by revisionId for fast lookup.
 */
@Component
public class WorkflowInstanceStore {

    private final Map<String, WorkflowInstance> byKey = new ConcurrentHashMap<>();
    private final Map<Long, List<String>> byRevision = new ConcurrentHashMap<>();

    public void save(WorkflowInstance instance) {
        byKey.put(instance.getProcessInstanceKey(), instance);
        byRevision.computeIfAbsent(instance.getRevisionId(), k -> new ArrayList<>())
                  .add(instance.getProcessInstanceKey());
    }

    public List<WorkflowInstance> findByRevision(Long revisionId) {
        List<String> keys = byRevision.getOrDefault(revisionId, List.of());
        return keys.stream().map(byKey::get).toList();
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
}
