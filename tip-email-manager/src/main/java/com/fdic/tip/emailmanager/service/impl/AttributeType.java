package gov.fdic.tip.emailmanager.constants;

/**
 * The four allowed values of contact_attribute.type (matches the DB CHECK
 * constraint). Centralized here instead of repeating the raw strings with
 * equalsIgnoreCase() checks scattered across services.
 */
public final class AttributeType {

    public static final String TEXT = "Text";
    public static final String NUMBER = "Number";
    public static final String DATE = "Date";
    public static final String FIXED_LIST = "Fixed List";

    private AttributeType() {
    }
}
