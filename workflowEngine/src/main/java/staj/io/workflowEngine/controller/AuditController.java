package staj.io.workflowEngine.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import staj.io.workflowEngine.dataAccess.ProcessAuditRepository;
import staj.io.workflowEngine.model.ProcessAudit;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/audit")
@CrossOrigin(origins = "*")
public class AuditController {

    private final ProcessAuditRepository auditRepository;

    public AuditController(ProcessAuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    @GetMapping
    public List<ProcessAudit> getAllAudit() {
        return auditRepository.findAll().stream()
                .sorted(Comparator.comparing(ProcessAudit::getTimestamp).reversed())
                .toList();
    }
}