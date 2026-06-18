package com.abccash.app.treasury.data

fun hasPermission(
    role: UserRole,
    permissions: Set<UserPermission>,
    permission: UserPermission
): Boolean = role == UserRole.ADMIN || permission in permissions

fun effectivePermissions(role: UserRole, permissions: Set<UserPermission>): Set<UserPermission> =
    if (role == UserRole.ADMIN) UserPermission.entries.toSet() else permissions
