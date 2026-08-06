package staj.io.workflowEngine.exception;

public class InvalidWorkflowDefinitionException extends RuntimeException {
    public InvalidWorkflowDefinitionException(String message) {
        super(message);
    }

    public InvalidWorkflowDefinitionException(String message, Throwable cause) {
        super(message, cause);
    }
}