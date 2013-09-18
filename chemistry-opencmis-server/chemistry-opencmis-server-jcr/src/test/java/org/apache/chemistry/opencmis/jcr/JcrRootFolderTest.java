package org.apache.chemistry.opencmis.jcr;

import org.apache.chemistry.opencmis.commons.data.AllowableActions;
import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.commons.enums.Action;
import org.junit.Assert;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.fail;

public class JcrRootFolderTest extends AbstractJcrSessionTest {

    private void checkAllowableAction(String actionName) {
        JcrRepository repository = getJcrRepository();

        ObjectData root = repository.getObjectByPath(getSession(),
                "/", null, true, false, null, false);

        AllowableActions allowableActions = root.getAllowableActions();

        Set<Action> allowableActionsSet = allowableActions.getAllowableActions();

        for (Action action : allowableActionsSet) {
            if (action.value().equals(actionName)) {
                fail("Root can't have allowableAction: " + actionName);
            }
        }
    }

    @Test
    public void rootObjectDataShouldNotContainMoveAction() {
        String actionName = "canMoveObject";
        checkAllowableAction(actionName);
    }

    @Test
    public void rootObjectDataShouldNotContainDeleteAction() {
        String actionName = "canDeleteObject";
        checkAllowableAction(actionName);
    }
}
