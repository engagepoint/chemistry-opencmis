package org.apache.chemistry.opencmis.jcr;

import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.commons.enums.Action;
import org.apache.chemistry.opencmis.commons.server.ObjectInfoHandler;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class ObjectDataCompiler {
    private boolean includeAllowableActions;
    private ObjectInfoHandler objectInfos;
    private boolean requiresObjectInfo;
    private Set<String> filters;
    private boolean nodeIsRoot;
    private ObjectData objectData;
    private List<Action> nonRootActions = Arrays.asList(Action.CAN_MOVE_OBJECT, Action.CAN_DELETE_OBJECT);

    public void setIncludeAllowableActions(boolean includeAllowableActions) {
        this.includeAllowableActions = includeAllowableActions;
    }

    public void setObjectInfos(ObjectInfoHandler objectInfos) {
        this.objectInfos = objectInfos;
    }

    public void setRequiresObjectInfo(boolean requiresObjectInfo) {
        this.requiresObjectInfo = requiresObjectInfo;
    }

    public void setFilters(Set<String> filters) {
        this.filters = filters;
    }

    public void setNodeIsRoot(boolean nodeIsRoot) {
        this.nodeIsRoot = nodeIsRoot;
    }

    public ObjectData compileObjectType(JcrNode jcrNode) {
        objectData = jcrNode.compileObjectType(filters, includeAllowableActions, objectInfos, requiresObjectInfo);
        if (nodeIsRoot) {
            removeNonAllowedActions();
        }

        return objectData;
    }

    private void removeNonAllowedActions() {
        Set<Action> allowableActions = objectData.getAllowableActions().getAllowableActions();
        for (Action nonRootAction : nonRootActions) {
            allowableActions.remove(nonRootAction);
        }
    }
}
