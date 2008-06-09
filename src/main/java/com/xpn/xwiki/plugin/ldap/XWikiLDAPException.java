package com.xpn.xwiki.plugin.ldap;

import com.xpn.xwiki.XWikiException;

/**
 * LDAP plugin base exception.
 * 
 * @version $Id$
 */
public class XWikiLDAPException extends XWikiException
{
    /**
     * Create new instance of LDAP exception.
     * 
     * @param message error message.
     */
    public XWikiLDAPException(String message)
    {
        super(XWikiException.MODULE_XWIKI_PLUGINS, XWikiException.ERROR_XWIKI_UNKNOWN, message);
    }
    
    /**
     * Create new instance of LDAP exception.
     * 
     * @param message error message.
     * @param e the wrapped exception.
     */
    public XWikiLDAPException(String message, Exception e)
    {
        super(XWikiException.MODULE_XWIKI_PLUGINS, XWikiException.ERROR_XWIKI_UNKNOWN, message, e);
    }
}
