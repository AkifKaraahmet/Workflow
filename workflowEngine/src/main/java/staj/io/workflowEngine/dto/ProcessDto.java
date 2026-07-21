package staj.io.workflowEngine.dto;

import lombok.Data;

@Data
public class ProcessDto {
    private String processName;

    private String processCode;
    
    private int version;

    private boolean isActive;
    
    private String startNodeId;

    private String definitionJson;

}