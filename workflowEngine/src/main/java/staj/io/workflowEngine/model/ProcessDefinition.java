package staj.io.workflowEngine.model;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "process_definition")
public class ProcessDefinition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY )
    private int id;

    @Column(name = "process_name")
    private String processName;

    @Column(name = "process_code")
    private String processCode;
    
    private int version;

    @Column(name = "is_active" ,nullable = false)
    private boolean isActive;
    
    @Column(name = "start_node_id")
    private String startNodeId;

    @Column(name = "definition_json" ,columnDefinition = "TEXT")
    private String definitionJson;

    public int getId() {
        return this.id;

}
}