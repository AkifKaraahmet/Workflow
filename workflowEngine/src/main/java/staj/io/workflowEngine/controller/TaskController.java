package staj.io.workflowEngine.controller;

import org.springframework.web.bind.annotation.*;
import staj.io.workflowEngine.dataAccess.ProcessTaskRepository;
import staj.io.workflowEngine.dataAccess.ProcessInstanceRepository;
import staj.io.workflowEngine.dataAccess.ProcessAuditRepository;
import staj.io.workflowEngine.exception.ResourceNotFoundException;
import staj.io.workflowEngine.exception.ConflictException;
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

        ProcessTask task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Görev bulunamadı: " + id));

        
        if ("COMPLETED".equals(task.getStatus())) {
            throw new ConflictException("Bu görev zaten tamamlanmış! (id=" + id + ")");
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

        ProcessInstance instance = instanceRepository.findById(task.getProcessInstanceId())
                .orElseThrow(() -> new ResourceNotFoundException("Süreç örneği bulunamadı: " + task.getProcessInstanceId()));

        engineService.completeUserTaskAndAdvance(instance, task.getTaskNodeId());

        return "Görev başarıyla tamamlandı, süreç bir sonraki adıma ilerletildi.";
    }

}