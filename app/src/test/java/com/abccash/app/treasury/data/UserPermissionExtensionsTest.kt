package com.abccash.app.treasury.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserPermissionExtensionsTest {

    @Test
    fun adminHasAllPermissions() {
        assertTrue(hasPermission(UserRole.ADMIN, emptySet(), UserPermission.MANAGE_USERS))
        assertTrue(hasPermission(UserRole.ADMIN, emptySet(), UserPermission.VIEW_TREASURY))
    }

    @Test
    fun staffRespectsGrantedPermissions() {
        val permissions = setOf(UserPermission.VIEW_INVOICES, UserPermission.ADD_PAYMENTS)
        assertTrue(hasPermission(UserRole.STAFF, permissions, UserPermission.VIEW_INVOICES))
        assertFalse(hasPermission(UserRole.STAFF, permissions, UserPermission.MANAGE_EXPENSES))
    }

    @Test
    fun effectivePermissions_adminGetsAll() {
        val effective = effectivePermissions(UserRole.ADMIN, setOf(UserPermission.VIEW_INVOICES))
        assertTrue(effective.containsAll(UserPermission.entries))
    }
}
