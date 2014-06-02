package org.eigengo.activator.nashorn;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;

public class WorkflowInstanceBridge extends AbstractMap<String, Object> implements WorkflowInstanceOperations {
    private final WorkflowInstanceOperations operations;
    private final Map<String, Object> environment;

    public WorkflowInstanceBridge(Map<String, Object> environment, WorkflowInstanceOperations operations) {
        this.operations = operations;
        this.environment = environment;
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return environment.entrySet();
    }

    @Override
    public void next(String stateName) {
        operations.next(stateName, null, null);
    }

    @Override
    public void next(String stateName, Object newData) {
        operations.next(stateName, newData, null);
    }

    @Override
    public void next(String stateName, Object newData, Object instruction) {
        operations.next(stateName, newData, instruction);
    }

    @Override
    public void end(Object newData, Object instruction) {
        operations.end(newData, instruction);
    }

    @Override
    public void end(Object newData) {
        operations.end(newData);
    }
}
