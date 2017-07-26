package org.ovirt.engine.core.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.ClassRule;
import org.junit.Test;
import org.ovirt.engine.core.common.businessentities.Role;
import org.ovirt.engine.core.common.businessentities.RoleType;
import org.ovirt.engine.core.common.mode.ApplicationMode;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.utils.MockConfigRule;

public class RoleDaoTest extends BaseDaoTestCase {
    @ClassRule
    public static MockConfigRule mcr = new MockConfigRule();

    private static final String GROUP_IDS = "26df4393-659b-4b8a-b0f6-3ee94d32e82f,08963ba9-b1c8-498d-989f-75cf8142eab7";
    private static final int ROLE_COUNT = 7;
    private static final int NON_ADMIN_ROLE_COUNT = 6;

    private RoleDao dao;
    private Role existingRole;
    private Role newRole;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        dao = dbFacade.getRoleDao();

        existingRole = dao.get(FixturesTool.ROLE_ID);

        newRole = new Role();
        newRole.setName("new role");
        newRole.setDescription("This is a new role.");
        newRole.setType(RoleType.USER);
        newRole.setAllowsViewingChildren(false);
        newRole.setAppMode(ApplicationMode.AllModes);
    }

    /**
     * Ensures that the id must be valid.
     */
    @Test
    public void testGetRoleWithInvalidId() {
        Role result = dao.get(Guid.newGuid());

        assertNull(result);
    }

    /**
     * Ensures that retrieving a role works as expected.
     */
    @Test
    public void testGetRole() {
        Role result = dao.get(existingRole.getId());

        assertNotNull(result);
        assertEquals(existingRole, result);
    }

    /**
     * Ensures that an invalid name results in a null role.
     */
    @Test
    public void testGetRoleByNameWithInvalidName() {
        Role result = dao.getByName("Farkle");

        assertNull(result);
    }

    /**
     * Ensures that retrieving a role by name works as expected.
     */
    @Test
    public void testGetRoleByName() {
        Role result = dao.getByName(existingRole.getName());

        assertNotNull(result);
        assertEquals(existingRole, result);
    }

    /**
     * Ensures the right number of roles are returned.
     */
    @Test
    public void testGetAllRoles() {
        List<Role> result = dao.getAll();

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(ROLE_COUNT, result.size());
    }

    /**
     * Ensures the right number of non-admin roles are returned.
     */
    @Test
    public void testGetAllNonAdminRoles() {
        List<Role> result = dao.getAllNonAdminRoles();

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(NON_ADMIN_ROLE_COUNT, result.size());
    }

    /**
     * Ensures an admin role is returned
     */
    @Test
    public void testAnyAdminRoleForUserAndGroups() {
        List<Role> result = dao.getAnyAdminRoleForUserAndGroups(PRIVILEGED_USER_ID,
                GROUP_IDS, ApplicationMode.AllModes.getValue());
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    /**
     * Ensures no admin role is returned
     */
    @Test
    public void testNoAdminRoleForUserAndGroups() {
        List<Role> result = dao.getAnyAdminRoleForUserAndGroups(UNPRIVILEGED_USER_ID,
                GROUP_IDS, ApplicationMode.AllModes.getValue());
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetAllForUsersAndGroupsInvalidUserAndGroups() {
        List<Role> result = dao.getAnyAdminRoleForUserAndGroups(Guid.newGuid(),
                Guid.newGuid().toString(), ApplicationMode.AllModes.getValue());
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /**
     * Ensures that saving a role works as expected.
     */
    @Test
    public void testSaveRole() {
        dao.save(newRole);

        Role result = dao.getByName(newRole.getName());

        assertNotNull(result);
        assertEquals(newRole, result);
    }

    /**
     * Ensures that updating a role works as expected.
     */
    @Test
    public void testUpdateRole() {
        existingRole.setDescription("This is an updated description");

        dao.update(existingRole);

        Role result = dao.get(existingRole.getId());

        assertNotNull(result);
        assertEquals(existingRole, result);
    }

    /**
     * Asserts removing a role works as expected
     */
    @Test
    public void testRemoveRole() {
        dao.remove(existingRole.getId());

        Role result = dao.get(existingRole.getId());

        assertNull(result);
    }
}
