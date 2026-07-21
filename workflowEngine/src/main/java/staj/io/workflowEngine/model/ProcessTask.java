package staj.io.workflowEngine.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "process_task")
public class ProcessTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "process_instance_id")
    private int processInstanceId;

    @Column(name = "task_node_id")
    private String taskNodeId;

    private String assignee;
    private String status;

    @Column(name = "completed_by")
    private String completedBy;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "completed_date")
    private LocalDate completedDate;

    // --- MANUEL GET/SET METODLARI ---
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getProcessInstanceId() { return processInstanceId; }
    public void setProcessInstanceId(int processInstanceId) { this.processInstanceId = processInstanceId; }

    public String getTaskNodeId() { return taskNodeId; }
    public void setTaskNodeId(String taskNodeId) { this.taskNodeId = taskNodeId; }

    public String getAssignee() { return assignee; }
    public void setAssignee(String assignee) { this.assignee = assignee; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCompletedBy() { return completedBy; }
    public void setCompletedBy(String completedBy) { this.completedBy = completedBy; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    public LocalDate getCompletedDate() { return completedDate; }
    public void setCompletedDate(LocalDate completedDate) { this.completedDate = completedDate; }
}