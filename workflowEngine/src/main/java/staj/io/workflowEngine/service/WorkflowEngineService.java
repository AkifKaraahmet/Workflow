package staj.io.workflowEngine.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staj.io.workflowEngine.dataAccess.ProcessInstanceRepository;
import staj.io.workflowEngine.dataAccess.ProcessTaskRepository;
import staj.io.workflowEngine.dataAccess.ProcessAuditRepository;
import staj.io.workflowEngine.dataAccess.ProcessDefinitionRepository;
import staj.io.workflowEngine.model.ProcessInstance;
import staj.io.workflowEngine.model.ProcessTask;
import staj.io.workflowEngine.model.ProcessAudit;
import staj.io.workflowEngine.model.ProcessDefinition;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class WorkflowEngineService {

    private final ProcessInstanceRepository instanceRepository;
    private final ProcessDefinitionRepository definitionRepository;
    private final ProcessTaskRepository taskRepository;
    private final ProcessAuditRepository auditRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WorkflowEngineService(ProcessInstanceRepository instanceRepository,ProcessDefinitionRepository definitionRepository,ProcessTaskRepository taskRepository,ProcessAuditRepository auditRepository) {
        this.instanceRepository = instanceRepository;
        this.definitionRepository = definitionRepository;
        this.taskRepository = taskRepository;
        this.auditRepository = auditRepository;
    }

    @Transactional
    public void completeUserTaskAndAdvance(ProcessInstance instance, String completedNodeId) {
        ProcessDefinition definition = definitionRepository.findById(instance.getProcessDefinitionId())
                .orElseThrow(() -> new RuntimeException("Süreç tanımı bulunamadı!"));

        try {
            JsonNode root = objectMapper.readTree(definition.getDefinitionJson());
            JsonNode transitions = root.get("transitions");

            String nextNodeId = null;
            if (transitions != null) {
                for (int i = 0; i < transitions.size(); i++) {
                    JsonNode t = transitions.get(i);
                    if (t.get("from").asText().equals(completedNodeId)) {
                        nextNodeId = t.get("to").asText();
                        break;
                    }
                }
            }

            if (nextNodeId == null) {
                throw new RuntimeException("Tamamlanan task'tan sonrası için transition bulunamadı: " + completedNodeId);
            }

            instance.setCurrentNodeId(nextNodeId);
            instanceRepository.save(instance);

            
            proceed(instance);

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Task sonrası süreç ilerletilirken hata oluştu: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void proceed(ProcessInstance instance) {
        if ("COMPLETED".equals(instance.getStatus()) || "CANCELLED".equals(instance.getStatus()) || "SUSPENDED".equals(instance.getStatus())) {
            return;
        }

        try {
            ProcessDefinition definition = definitionRepository.findById(instance.getProcessDefinitionId())
                    .orElseThrow(() -> new RuntimeException("Süreç tanımı bulunamadı!"));

            JsonNode root = objectMapper.readTree(definition.getDefinitionJson());
            JsonNode nodes = root.get("nodes");
            JsonNode transitions = root.get("transitions");

            JsonNode currentNode = null;
            if (nodes != null) {
                for (int i = 0; i < nodes.size(); i++) {
                    JsonNode n = nodes.get(i);
                    if (n.get("id").asText().equals(instance.getCurrentNodeId())) {
                        currentNode = n;
                        break;
                    }
                }
            }

            if (currentNode == null) return;

            //user task adımında bekle
            if ("USER_TASK".equals(currentNode.get("type").asText())) {
                return;
            }

            String nextNodeId = null;
            if (transitions != null) {
                for (int i = 0; i < transitions.size(); i++) {
                    JsonNode t = transitions.get(i);
                    if (t.get("from").asText().equals(instance.getCurrentNodeId())) {

                        if (t.has("condition")) {
                            boolean isConditionMet = evaluateCondition(t.get("condition"), instance.getVariablesJson());
                            if (isConditionMet) {
                                nextNodeId = t.get("to").asText();
                                break;
                            }
                        }
                    }
                }

                if (nextNodeId == null) {
                    for (int i = 0; i < transitions.size(); i++) {
                        JsonNode t = transitions.get(i);
                        if (t.get("from").asText().equals(instance.getCurrentNodeId())) {

                            if (t.has("default") && t.get("default").asBoolean()) {
                                nextNodeId = t.get("to").asText();
                                break;
                            } else if (!t.has("condition")) {
                                nextNodeId = t.get("to").asText();
                                break;
                            }
                        }
                    }
                }
            }

            if (nextNodeId == null) {
                if ("END".equals(currentNode.get("type").asText())) {
                    instance.setStatus("COMPLETED");
                    instance.setEndDate(LocalDateTime.now());
                    instanceRepository.save(instance);
                    saveAudit(instance.getId(), instance.getCurrentNodeId(), "COMPLETED", "Süreç tamamlandı.", "SYSTEM");
                }
                return;
            }


            instance.setCurrentNodeId(nextNodeId);
            instanceRepository.save(instance);
            JsonNode nextNode = null;
            if (nodes != null) {
                for (int i = 0; i < nodes.size(); i++) {
                    JsonNode n = nodes.get(i);
                    if (n.get("id").asText().equals(nextNodeId)) {
                        nextNode = n;
                        break;
                    }
                }
            }

            if (nextNode == null) return;

            String nextNodeType = nextNode.get("type").asText();

            if ("USER_TASK".equals(nextNodeType)) {

                final String finalNextNodeId = nextNodeId;
                List<ProcessTask> existingTasks = taskRepository.findAll();
                boolean alreadyHasTask = existingTasks.stream()
                        .anyMatch(t -> t.getProcessInstanceId() == instance.getId()
                                    && t.getTaskNodeId().equals(finalNextNodeId)
                                    && "PENDING".equalsIgnoreCase(t.getStatus()));

                if (!alreadyHasTask) {
                    ProcessTask task = new ProcessTask();
                    task.setProcessInstanceId(instance.getId());
                    task.setTaskNodeId(nextNode.get("id").asText());
                    task.setAssignee(nextNode.has("assignee") ? nextNode.get("assignee").asText() : "UNASSIGNED");
                    task.setStatus("PENDING");
                    task.setCreatedDate(LocalDateTime.now());
                    taskRepository.save(task);

                    saveAudit(instance.getId(), nextNodeId, "TASK_CREATED", "Onay görevi oluşturuldu. Atanan rol: " + task.getAssignee(), "SYSTEM");
                }

            } else if ("SERVICE_TASK".equals(nextNodeType)) {
                saveAudit(instance.getId(), nextNodeId, "SERVICE_EXECUTION", "Sistem adımı çalıştırıldı.", "SYSTEM");
                proceed(instance);
            } else if ("END".equals(nextNodeType)) {
                instance.setStatus("COMPLETED");
                instance.setEndDate(LocalDateTime.now());
                instanceRepository.save(instance);
                saveAudit(instance.getId(), nextNodeId, "COMPLETED", "Süreç sonlandı.", "SYSTEM");
            } else {
                proceed(instance);
            }

        } catch (Exception e) {
            throw new RuntimeException("Süreç ilerletilirken hata oluştu: " + e.getMessage(), e);
        }
    }

    private void saveAudit(int instanceId, String nodeId, String action, String message, String actor) {
        ProcessAudit audit = new ProcessAudit();
        audit.setProcessInstanceId(instanceId);
        audit.setNodeId(nodeId);
        audit.setAction(action);
        audit.setMessages(message);
        audit.setTimestamp(LocalDateTime.now());
        audit.setActor(actor);
        auditRepository.save(audit);
    }

    private boolean evaluateCondition(JsonNode conditionNode, String variablesJsonStr) {
        try {
            if (variablesJsonStr == null || conditionNode == null) return false;
            Map<String, Object> vars = objectMapper.readValue(variablesJsonStr, Map.class);
            String varName = conditionNode.get("variable").asText();
            String operator = conditionNode.get("operator").asText();
            JsonNode valueNode = conditionNode.get("value");

            Object rawValue = vars.get(varName);
            if (rawValue == null) return false;

            int currentValue = Integer.parseInt(rawValue.toString());
            int targetValue = valueNode.asInt();

            if ("gt".equals(operator)) {
                return currentValue > targetValue;
            } else if ("lt".equals(operator)) {
                return currentValue < targetValue;
            } else if ("eq".equals(operator)) {
                String currentValueStr = rawValue.toString();
                String targetValueStr = valueNode.asText();
                return currentValueStr.equalsIgnoreCase(targetValueStr);
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}