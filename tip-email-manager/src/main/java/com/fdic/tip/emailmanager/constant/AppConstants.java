package com.fdic.tip.emailmanager.constant;

public final class AppConstants {

    private AppConstants() {}

    // Security Roles
    public static final String ROLE_ADMIN = "ROLE_TIP_ADMINISTRATOR";
    public static final String ROLE_AUTHOR = "ROLE_TEMPLATE_AUTHOR";
    public static final String HAS_ROLE_ADMIN = "hasRole('" + ROLE_ADMIN + "')";

    // Status Values
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_INACTIVE = "INACTIVE";

    // Error Messages
    public static final String ERR_NAME_EXISTS = "Data Connection Name already exists.";
    public static final String ERR_NOT_FOUND = "Data Connection not found with ID: ";
    public static final String ERR_CANNOT_DELETE_USED = "Connection cannot be deleted as it is associated with email templates.";
    public static final String ERR_CANNOT_DELETE_ACTIVE = "Delete is only allowed on inactive data connections.";
    public static final String ERR_CONNECTION_FAILED = "Failed to connect to the target database location.";

    // Success Messages
    public static final String MSG_TEST_SUCCESS = "Database connection test successful.";
    public static final String MSG_DELETED_SUCCESS = "Data connection deleted successfully.";
	public static final String MSG_CONNECTION_DELETED = "";
	public static final String MSG_DOMAIN_DELETED = null;


    // Audit Log Actions
    public static final String ACTION_CREATE = "CREATE";
    public static final String ACTION_UPDATE = "UPDATE";
    public static final String ACTION_DELETE = "DELETE";
    public static final String ACTION_STATUS_CHANGE = "STATUS_CHANGE";
	public static final Long ERR_LOCATION_NOT_FOUND = null;
	
	
	// Data Connection Permissions
    public static final String PERM_DATA_CONNECTION_ADD = "hasAuthority('EM_DATA_CONNECTION_ADD')";
    public static final String PERM_DATA_CONNECTION_EDIT = "hasAuthority('EM_DATA_CONNECTION_EDIT')";
    public static final String PERM_DATA_CONNECTION_DELETE = "hasAuthority('EM_DATA_CONNECTION_DELETE')";
    public static final String PERM_DATA_CONNECTION_VIEW = "hasAuthority('EM_DATA_CONNECTION_VIEW')";
    public static final String PERM_DATA_CONNECTION_TEST = "hasAuthority('EM_DATA_CONNECTION_TEST')";

    // No-Reply Mailbox Permissions
    public static final String PERM_NOREPLY_ADD = "hasAuthority('EM_NOREPLY_ADD')";
    public static final String PERM_NOREPLY_EDIT = "hasAuthority('EM_NOREPLY_EDIT')";
    public static final String PERM_NOREPLY_VIEW = "hasAuthority('EM_NOREPLY_VIEW')";
    public static final String PERM_NOREPLY_DELETE = "hasAuthority('EM_NOREPLY_DELETE')";
    public static final String PERM_NOREPLY_SAVE_OR_UPDATE = "hasAnyAuthority('EM_NOREPLY_ADD', 'EM_NOREPLY_EDIT')";

    // Internal Domain Allowlist Permissions
    public static final String PERM_DOMAIN_ADD = "hasAuthority('EM_DOMAIN_ADD')";
    public static final String PERM_DOMAIN_EDIT = "hasAuthority('EM_DOMAIN_EDIT')";
    public static final String PERM_DOMAIN_DELETE = "hasAuthority('EM_DOMAIN_DELETE')";
    public static final String PERM_DOMAIN_VIEW = "hasAuthority('EM_DOMAIN_VIEW')";
    
    
 // Roles permitted: Sr. Analyst, Manager, System Admin
    public static final String PERM_CONTACT_VIEW   = "hasAnyAuthority('EM_CONTACT_VIEW', 'ROLE_SR_ANALYST', 'ROLE_MANAGER', 'ROLE_SYSTEM_ADMIN')";
    public static final String PERM_CONTACT_ADD    = "hasAnyAuthority('EM_CONTACT_ADD', 'ROLE_SR_ANALYST', 'ROLE_MANAGER', 'ROLE_SYSTEM_ADMIN')";
    public static final String PERM_CONTACT_EDIT   = "hasAnyAuthority('EM_CONTACT_EDIT', 'ROLE_SR_ANALYST', 'ROLE_MANAGER', 'ROLE_SYSTEM_ADMIN')";
    public static final String PERM_CONTACT_DELETE = "hasAnyAuthority('EM_CONTACT_DELETE', 'ROLE_SR_ANALYST', 'ROLE_MANAGER', 'ROLE_SYSTEM_ADMIN')";
    
 // Contact Attribute Registry Permissions
    public static final String PERM_CONTACT_ATTRIBUTE_VIEW   = "hasAuthority('EM_CONTACT_ATTRIBUTE_VIEW')";
    public static final String PERM_CONTACT_ATTRIBUTE_ADD    = "hasAuthority('EM_CONTACT_ATTRIBUTE_ADD')";
    public static final String PERM_CONTACT_ATTRIBUTE_EDIT   = "hasAuthority('EM_CONTACT_ATTRIBUTE_EDIT')";
    public static final String PERM_CONTACT_ATTRIBUTE_DELETE = "hasAuthority('EM_CONTACT_ATTRIBUTE_DELETE')";
    
    
 // Approved Sender Permissions mapped explicitly per action
    public static final String PERM_APPROVED_SENDER_VIEW   = "hasAuthority('EM_APPROVED_SENDER_VIEW')";
    public static final String PERM_APPROVED_SENDER_ADD    = "hasAuthority('EM_APPROVED_SENDER_ADD')";
    public static final String PERM_APPROVED_SENDER_EDIT   = "hasAuthority('EM_APPROVED_SENDER_EDIT')";
    public static final String PERM_APPROVED_SENDER_DELETE = "hasAuthority('EM_APPROVED_SENDER_DELETE')";
    
    
}