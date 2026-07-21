package staj.io.workflowEngine.controller;

import org.springframework.web.bind.annotation.*;
import staj.io.workflowEngine.dataAccess.ProcessDefinitionRepository;
import staj.io.workflowEngine.dataAccess.ProcessInstanceRepository;
import staj.io.workflowEngine.dataAccess.ProcessAuditRepository;
import staj.io.workflowEngine.model.ProcessDefinition;
import staj.io.workflowEngine.model.ProcessInstance;
import staj.io.workflowEngine.model.ProcessAudit;
import staj.io.workflowEngine.service.WorkflowEngineService;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/instances")
@CrossOrigin(origins = "*")
public class InstanceController {

    private final ProcessInstanceRepository instanceRepository;
    private final ProcessDefinitionRepository definitionRepository;
    private final ProcessAuditRepository auditRepository;
    private final WorkflowEngineService engineService;

    public InstanceController(ProcessInstanceRepository instanceRepository,
                              ProcessDefinitionRepository definitionRepository,
                              ProcessAuditRepository auditRepository,
                              WorkflowEngineService engineService) {
        this.instanceRepository = instanceRepository;
        this.definitionRepository = definitionRepository;
        this.auditRepository = auditRepository;
        this.engineService = engineService;
    }


    @PostMapping("/start/{processCode}")
    public ProcessInstance startInstance(@PathVariable String processCode, @RequestBody(required = false) String variablesJson) {
        List<ProcessDefinition> definitions = definitionRepository.findAll();
        ProcessDefinition activeDef = null;
        for (ProcessDefinition def : definitions) {
            if (def.getProcessCode().equals(processCode) && def.isActive()) {
                activeDef = def;
                break;
            }
        }
        
        if (activeDef == null) {
            throw new RuntimeException("Aktif süreç şablonu bulunamadı: " + processCode);
        }

        ProcessInstance instance = new ProcessInstance();
        instance.setProcessDefinitionId(activeDef.getId()); 
        instance.setStatus("STARTED");
        instance.setCurrentNodeId(activeDef.getStartNodeId());
        
        instance.setVariablesJson(variablesJson != null ? variablesJson : "{}");
        
        instance.setCreatedDate(LocalDateTime.now());
        
        ProcessInstance savedInstance = instanceRepository.save(instance);

        ProcessAudit audit = new ProcessAudit();
        audit.setProcessInstanceId(savedInstance.getId());
        audit.setNodeId(activeDef.getStartNodeId());
        audit.setAction("STARTED");
        audit.setMessages("Süreç başarıyla başlatıldı.");
        audit.setTimestamp(LocalDateTime.now());
        audit.setActor("SYSTEM");
        
        auditRepository.save(audit);

        engineService.proceed(savedInstance);

        return instanceRepository.findById(savedInstance.getId()).orElse(savedInstance);
    }

    @GetMapping("/{id}")
    public ProcessInstance getInstance(@PathVariable int id) {
        return instanceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Süreç örneği bulunamadı: " + id));
    }

    @GetMapping("/{id}/variables")
    public String getVariables(@PathVariable int id) {
        return instanceRepository.findById(id)
                .map(ProcessInstance::getVariablesJson)
                .orElseThrow(() -> new RuntimeException("Süreç örneği bulunamadı: " + id));
    }

    @PostMapping("/{id}/cancel")
    public String cancelInstance(@PathVariable int id) {
        ProcessInstance instance = instanceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Süreç örneği bulunamadı: " + id));
        
        if ("COMPLETED".equals(instance.getStatus()) || "CANCELLED".equals(instance.getStatus())) {
            throw new RuntimeException("Zaten tamamlanmış veya iptal edilmiş süreç tekrar iptal edilemez!");
        }

        instance.setStatus("CANCELLED");
        instance.setEndDate(LocalDateTime.now());
        instanceRepository.save(instance);

        ProcessAudit audit = new ProcessAudit();
        audit.setProcessInstanceId(id);
        audit.setNodeId(instance.getCurrentNodeId()); 
        audit.setAction("CANCELLED");
        audit.setMessages("Süreç kullanıcı tarafından iptal edildi."); 
        audit.setTimestamp(LocalDateTime.now());
        audit.setActor("USER");
        auditRepository.save(audit);

        return "Süreç iptal edildi.";
    }

    @PostMapping("/{id}/suspend")
    public String suspendInstance(@PathVariable int id) {
        ProcessInstance instance = instanceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Süreç örneği bulunamadı: " + id));
        
        if ("COMPLETED".equals(instance.getStatus()) || "CANCELLED".equals(instance.getStatus())) {
            throw new RuntimeException("Zaten tamamlanmış veya iptal edilmiş süreç dondurulamaz!");
        }

        instance.setStatus("SUSPENDED");
        instanceRepository.save(instance);

        ProcessAudit audit = new ProcessAudit();
        audit.setProcessInstanceId(id);
        audit.setNodeId(instance.getCurrentNodeId());
        audit.setAction("SUSPENDED");
        audit.setMessages("Süreç askıya alındı."); 
        audit.setTimestamp(LocalDateTime.now());
        audit.setActor("SYSTEM");
        auditRepository.save(audit);

        return "Süreç askıya alındı.";
    }

    @PostMapping("/{id}/resume")
    public String resumeInstance(@PathVariable int id) {
        ProcessInstance instance = instanceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Süreç örneği bulunamadı: " + id));
        
        if (!"SUSPENDED".equals(instance.getStatus())) {
            throw new RuntimeException("Sadece askıdaki (SUSPENDED) süreçler aktif edilebilir!");
        }

        instance.setStatus("STARTED");
        instanceRepository.save(instance);

        ProcessAudit audit = new ProcessAudit();
        audit.setProcessInstanceId(id);
        audit.setNodeId(instance.getCurrentNodeId());
        audit.setAction("RESUMED");
        audit.setMessages("Süreç askıdan indirildi, devam ediyor."); 
        audit.setTimestamp(LocalDateTime.now());
        audit.setActor("SYSTEM");
        auditRepository.save(audit);

        return "Süreç aktif edildi.";
    }

    @GetMapping("/{id}/history")
    public List<ProcessAudit> getHistory(@PathVariable int id) {
        List<ProcessAudit> allAudits = auditRepository.findAll();
        return allAudits.stream()
                .filter(audit -> audit.getProcessInstanceId() == id)
                .toList();
    }
}