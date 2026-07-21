package staj.io.workflowEngine.dataAccess;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import staj.io.workflowEngine.model.ProcessTask;

@Repository
public interface ProcessTaskRepository extends JpaRepository<ProcessTask, Integer> {
} 