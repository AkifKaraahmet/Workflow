package staj.io.workflowEngine.model;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "process_instance")
@Data
public class ProcessInstance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "process_definition_id")
    private int processDefinitionId;
    
    private String status;

    @Column(name = "current_node_id")
    private String currentNodeId;

    @Column(name = "variables_json", columnDefinition = "TEXT")
    private String variablesJson; 

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getProcessDefinitionId() { return processDefinitionId; }
    public void setProcessDefinitionId(int processDefinitionId) { this.processDefinitionId = processDefinitionId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCurrentNodeId() { return currentNodeId; }
    public void setCurrentNodeId(String currentNodeId) { this.currentNodeId = currentNodeId; }

    public String getVariablesJson() { return variablesJson; }
    public void setVariablesJson(String variablesJson) { this.variablesJson = variablesJson; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
}