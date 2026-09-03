package gov.fdic.tip.emailmanager.constants;

/**
 * Canonical status values for all status-bearing entities in this module
 * (contact, contact_attribute, approved_sender, internal_domain_allowlist,
 * data_connection). The schema's CHECK constraints only accept these two
 * uppercase values — do not introduce mixed-case literals ("Active",
 * "Inactive") anywhere in the service layer; use these constants instead.
 *
 * If a module-wide constants/enum class already exists elsewhere in the
 * codebase, fold these values into it rather than keeping this as a
 * separate file — the point is one source of truth, not this specific class.
 */
public final class ContactStatus {

    public static final String ACTIVE = "ACTIVE";
    public static final String INACTIVE = "INACTIVE";

    private ContactStatus() {
    }
}
