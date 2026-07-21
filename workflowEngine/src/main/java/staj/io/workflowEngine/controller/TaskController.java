package staj.io.workflowEngine.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import staj.io.workflowEngine.dataAccess.ProcessTaskRepository;
import staj.io.workflowEngine.dataAccess.ProcessInstanceRepository;
import staj.io.workflowEngine.dataAccess.ProcessAuditRepository;
import staj.io.workflowEngine.model.ProcessTask;
import staj.io.workflowEngine.model.ProcessInstance;
import staj.io.workflowEngine.model.ProcessAudit;
import staj.io.workflowEngine.service.WorkflowEngineService;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/tasks")
@CrossOrigin(origins = "*")
public class TaskController {

    private final ProcessTaskRepository taskRepository;
    private final ProcessInstanceRepository instanceRepository;
    private final ProcessAuditRepository auditRepository;
    private final WorkflowEngineService engineService;

    public TaskController(ProcessTaskRepository taskRepository,
                          ProcessInstanceRepository instanceRepository,
                          ProcessAuditRepository auditRepository,
                          WorkflowEngineService engineService) {
        this.taskRepository = taskRepository;
        this.instanceRepository = instanceRepository;
        this.auditRepository = auditRepository;
        this.engineService = engineService;
    }

    @GetMapping
    public List<ProcessTask> getTasksByAssignee(@RequestParam String assignee) {
        List<ProcessTask> allTasks = taskRepository.findAll();
        return allTasks.stream()
                .filter(task -> task.getAssignee().equalsIgnoreCase(assignee) && "PENDING".equalsIgnoreCase(task.getStatus()))
                .toList();
    }

    @PostMapping("/{id}/complete")
    public String completeTask(@PathVariable int id, @RequestBody(required = false) String completedBy) {
        
        // SADECE STANDART RUNTIMEEXCEPTION KULLANILDI!
        ProcessTask task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Görev bulunamadı: " + id));

        // SADECE STANDART RUNTIMEEXCEPTION KULLANILDI!
        if ("COMPLETED".equals(task.getStatus())) {
            throw new RuntimeException("Bu görev zaten tamamlanmış!");
        }

        task.setStatus("COMPLETED");
        task.setCompletedDate(java.time.LocalDate.now()); 
        
        if (completedBy != null && !completedBy.isEmpty()) {
            task.setCompletedBy(completedBy);
        } else {
            task.setCompletedBy(task.getAssignee());
        }
        taskRepository.save(task);

        ProcessAudit audit = new ProcessAudit();
        audit.setProcessInstanceId(task.getProcessInstanceId());
        audit.setNodeId(task.getTaskNodeId());
        audit.setAction("APPROVED");
        audit.setMessages("Görev " + task.getCompletedBy() + " tarafından onaylandı.");
        audit.setTimestamp(LocalDateTime.now());
        audit.setActor(task.getCompletedBy());
        auditRepository.save(audit);

        // SADECE STANDART RUNTIMEEXCEPTION KULLANILDI!
        ProcessInstance instance = instanceRepository.findById(task.getProcessInstanceId())
                .orElseThrow(() -> new RuntimeException("Süreç örneği bulunamadı!"));
        
        engineService.proceed(instance);

        return "Görev başarıyla tamamlandı, süreç bir sonraki adıma ilerletildi.";
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String handleOptimisticLockingFailure() {
        return "Hata: Bu görev başka bir yönetici tarafından az önce onaylandı (409)!";
    }
}