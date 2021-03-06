package org.ovirt.engine.core.bll.aaa;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import org.junit.Before;
import org.junit.Test;
import org.ovirt.engine.core.bll.BaseCommandTest;
import org.ovirt.engine.core.common.action.IdParameters;
import org.ovirt.engine.core.common.errors.EngineMessage;
import org.ovirt.engine.core.compat.Guid;

public class RemoveAdGroupCommandTest extends BaseCommandTest {

    /**
     * The command under test.
     */
    private RemoveGroupCommand<IdParameters> command;
    private Guid adElementId = Guid.newGuid();

    @Before
    public void initializeCommand() {
        IdParameters parameters = createParameters();
        command = spy(new RemoveGroupCommand<>(parameters, null));
    }

    /**
     * @return Valid parameters for the command.
     */
    private IdParameters createParameters() {
        return new IdParameters(adElementId);
    }

    @Test
    public void validateFailsOnRemoveLastAdGroupWithSuperUserPrivileges() throws Exception {
        mockIsLastSuperUserGroup(true);
        assertFalse(command.validate());
        assertTrue(command.getReturnValue().getValidationMessages().contains(
                EngineMessage.ERROR_CANNOT_REMOVE_LAST_SUPER_USER_ROLE.toString()));
    }

    @Test
    public void validateSucceedsOnRemoveNotLastAdGroupWithSuperUserPrivileges() throws Exception {
        mockIsLastSuperUserGroup(false);
        assertTrue(command.validate());
    }

    private void mockIsLastSuperUserGroup(boolean isLast) {
        doReturn(isLast).when(command).isLastSuperUserGroup(adElementId);
    }
}
