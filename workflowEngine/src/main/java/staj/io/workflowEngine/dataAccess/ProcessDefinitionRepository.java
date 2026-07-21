package staj.io.workflowEngine.dataAccess;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import staj.io.workflowEngine.model.ProcessDefinition;


@Repository
public interface ProcessDefinitionRepository extends JpaRepository<ProcessDefinition, Integer>{
    List<ProcessDefinition> findByProcessCode(String processCode);
}

