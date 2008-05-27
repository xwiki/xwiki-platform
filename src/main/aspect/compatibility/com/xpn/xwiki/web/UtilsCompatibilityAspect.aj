package com.xpn.xwiki.web;

import com.xpn.xwiki.XWikiContext;

/**
 * Add a backward compatibility layer to the {@link Utils} class.
 * 
 * @version $Id: $
 */
public aspect UtilsCompatibilityAspect
{
    /**
     * Lookup a XWiki component by role and hint.
     *
     * @param role the component's identity (usually the component's interface name as a String)
     * @param hint a value to differentiate different component implementations for the same role
     * @param context the XWiki Context where the Component Manager is stored
     * @return the component's Object
     */
    public static Object Utils.getComponent(String role, String hint, XWikiContext context)
    {
        return getComponent(role, hint);
    }

    /**
     * Lookup a XWiki component by role (uses the default hint).
     *
     * @param role the component's identity (usually the component's interface name as a String)
     * @param context the XWiki Context where the Component Manager is stored
     * @return the component's Object
     */
    public static Object Utils.getComponent(String role, XWikiContext context)
    {
        return getComponent(role);
    }
}
