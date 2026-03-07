package com.plm.workflow;

import com.plm.workflow.dto.PendingTask;
import com.plm.workflow.dto.WorkflowInstance;
import com.plm.workflow.service.WorkflowInstanceStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import io.camunda.zeebe.client.ZeebeClient;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WorkflowControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean JwtDecoder jwtDecoder;
    @MockBean ZeebeClient zeebeClient;
    @MockBean WorkflowInstanceStore store;

    @Test
    void getAll_returnsList() throws Exception {
        WorkflowInstance inst = new WorkflowInstance(
                "key1", 1L, 1L, "Motor Controller", "A", "admin",
                "RUNNING", "MANAGER_REVIEW", Instant.now());
        when(store.findAll()).thenReturn(List.of(inst));

        mockMvc.perform(get("/api/workflows").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].processInstanceKey").value("key1"))
                .andExpect(jsonPath("$[0].currentStep").value("MANAGER_REVIEW"));
    }

    @Test
    void getPendingTasks_returnsList() throws Exception {
        PendingTask task = new PendingTask(
                999L, "MANAGER_REVIEW", "key1",
                1L, 1L, "Motor Controller", "A", "admin", Instant.now());
        when(store.findAllPendingTasks()).thenReturn(List.of(task));

        mockMvc.perform(get("/api/workflows/tasks").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].taskType").value("MANAGER_REVIEW"))
                .andExpect(jsonPath("$[0].jobKey").value(999));
    }

    @Test
    void getByRevision_returnsFiltered() throws Exception {
        WorkflowInstance inst = new WorkflowInstance(
                "key2", 5L, 2L, "BMS", "B", "engineer",
                "COMPLETED", "RELEASED", Instant.now());
        when(store.findByRevision(5L)).thenReturn(List.of(inst));

        mockMvc.perform(get("/api/workflows/revisions/5").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));
    }
}
