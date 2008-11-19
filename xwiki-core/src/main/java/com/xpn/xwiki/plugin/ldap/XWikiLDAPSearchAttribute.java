package com.xpn.xwiki.plugin.ldap;

/**
 * Represent an LDAP attribute.
 * 
 * @version $Id$
 * @since 1.3 M2
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

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return "{name=" + name + " value=" + value + "}";
    }
}
