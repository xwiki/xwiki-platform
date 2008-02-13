package com.xpn.xwiki.plugin.ldap;

/**
 * Represent an LDAP attribute.
 * 
 * @version $Id: $
 */
public class XWikiLDAPSearchAttribute
{
    /**
     * Attribute name.
     */
    public String name;

    /**
     * Attribute value.
     */
    public String value;

    /**
     * Create attribute instance.
     * 
     * @param name attribute name.
     * @param value attribute value.
     */
    public XWikiLDAPSearchAttribute(String name, String value)
    {
        this.name = name;
        this.value = value;
    }
}
