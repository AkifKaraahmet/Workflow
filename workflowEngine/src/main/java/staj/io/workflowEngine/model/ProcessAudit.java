package staj.io.workflowEngine.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "process_audit")
public class ProcessAudit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "process_instance_id")
    private int processInstanceId;

    @Column(name = "node_id")
    private String nodeId;

    private String action;

    @Column(columnDefinition = "TEXT")
    private String messages;

    private LocalDateTime timestamp;
    private String actor;

    
    public int getId() { 
        return id; 
    }
    public void setId(int id) {
        this.id = id;
    }

    public int getProcessInstanceId() { 
        return processInstanceId;
    }
    public void setProcessInstanceId(int processInstanceId) {
        this.processInstanceId = processInstanceId; 
    }

    public String getNodeId() { 
        return nodeId; 
    }
    public void setNodeId(String nodeId) { 
        this.nodeId = nodeId; 
    }

    public String getAction() { 
        return action; }
    public void setAction(String action) {
        this.action = action;
        }

    public String getMessages() {
        return messages;
        }
    public void setMessages(String messages) {
        this.messages = messages; 
        }

    public LocalDateTime getTimestamp() {
        return timestamp; 
        }
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp; 
        }

    public String getActor() { 
        return actor; 
    }
    public void setActor(String actor) {
        this.actor = actor; 
        }
}