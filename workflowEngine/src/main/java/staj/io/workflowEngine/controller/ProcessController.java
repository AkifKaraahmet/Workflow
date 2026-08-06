package staj.io.workflowEngine.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.*;
import staj.io.workflowEngine.model.ProcessDefinition;
import staj.io.workflowEngine.dto.ProcessDto;
import staj.io.workflowEngine.service.ProcessDefinitionService;
import java.util.List;
import staj.io.workflowEngine.dataAccess.ProcessDefinitionRepository;

@RestController
@RequestMapping("/definitions")
@CrossOrigin(origins = "*")
public class ProcessController {

    private final ProcessDefinitionService service;
    private final ProcessDefinitionRepository repository;

    public ProcessController(ProcessDefinitionService service, ProcessDefinitionRepository repository) {
        this.service = service;
        this.repository = repository;
    }

    @PostMapping
    public ProcessDefinition createDefinition(@RequestBody ProcessDto dto) throws Exception {

        ProcessDefinition definition = new ProcessDefinition();
        definition.setProcessName(dto.getProcessName());
        definition.setProcessCode(dto.getProcessCode());
        definition.setVersion(dto.getVersion());
        definition.setActive(dto.isActive());
        definition.setStartNodeId(dto.getStartNodeId());
        definition.setDefinitionJson(dto.getDefinitionJson());

        return service.deployProcess(definition);
    }

    @GetMapping
    public List<ProcessDefinition> getAllDefinitions() {
        return repository.findAll();
    }
}