package ch.hug.mtpgenerator.commons.specs;

/**
 * URNs defined by HL7.
 */
public class Hl7Urns {

    /**
     * HL7 v3 base URN.
     */
    public static final String HL7V3_URN = "urn:hl7-org:v3";

    /**
     * Instance Identifier Data Type.
     * <p>
     * An Instance Identifier shall have the XML element InstanceIdentifier with the XML attribute root and may have the
     * XML attribute extension. extension may be any string. root shall be an OID. An Instance Identifier may also have
     * the XML attribute assigningAuthorityName and displayable. assigningAuthorityName may be any string. displayable
     * must be either {@link true} or {@link false} if it exists.
     */
    public static final String DATA_TYPE_II = HL7V3_URN + "#II";

    /**
     * Instance Identifier Comparison Function.
     * <p>
     * This function shall take two arguments of data-type <pre>urn:hl7-org:v3#II</pre> and shall return a boolean. The
     * function shall return {@link true} if:
     * <li>the extension attribute is empty and the root attribute of both of its arguments are equal according to the
     * function <pre>urn:oasis:names:tc:xacml:1.0:function:string-equal</pre>; or,
     * <li>the extension attribute of both of its arguments are equal according to the function
     * <pre>urn:oasis:names:tc:xacml:1.0:function:string-equal</pre>, and the root attribute of
     * both of its arguments are equal according to the function <pre>urn:oasis:names:tc:xacml:1.0:function:string-equal</pre>.
     */
    public static final String FUNCTION_II_EQUAL = HL7V3_URN + ":function:II-equal";

    private Hl7Urns() {
    }
}
