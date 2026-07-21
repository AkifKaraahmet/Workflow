package staj.io.workflowEngine.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import staj.io.workflowEngine.dataAccess.ProcessDefinitionRepository;
import staj.io.workflowEngine.model.ProcessDefinition;

import java.util.List;

@Service
public class ProcessDefinitionService {

    private final ProcessDefinitionRepository definitionRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ProcessDefinitionService(ProcessDefinitionRepository definitionRepository) {
        this.definitionRepository = definitionRepository;
    }

    public ProcessDefinition deployProcess(ProcessDefinition definition2) throws Exception {
        JsonNode root = objectMapper.readTree(definition2.getDefinitionJson());

        ProcessDefinition definition = new ProcessDefinition();
        definition.setProcessCode(root.get("key") != null ? root.get("key").asText() : "unknown");
        definition.setProcessName(root.get("name") != null ? root.get("name").asText() : "Bilinmeyen Süreç");
        definition.setStartNodeId(root.get("startNode") != null ? root.get("startNode").asText() : "start");
        definition.setDefinitionJson(definition2.getDefinitionJson());

        List<ProcessDefinition> activeDefinitions = definitionRepository.findByProcessCode(definition.getProcessCode());
        int targetVersion = 1;
        for (ProcessDefinition old : activeDefinitions) {
            if (old.isActive()) {
                old.setActive(false);
                definitionRepository.save(old);
                targetVersion = old.getVersion() + 1;
            }
        }

        definition.setVersion(targetVersion);
        definition.setActive(true);

        return definitionRepository.save(definition);
    }
}