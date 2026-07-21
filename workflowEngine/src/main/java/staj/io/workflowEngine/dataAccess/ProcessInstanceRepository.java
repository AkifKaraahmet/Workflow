package staj.io.workflowEngine.dataAccess;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import staj.io.workflowEngine.model.ProcessInstance;

@Repository
public interface ProcessInstanceRepository extends JpaRepository<ProcessInstance, Integer> {
}