package com.xpn.xwiki.plugin.rightsmanager.utils;

import java.util.Collection;
import java.util.HashSet;

/**
 * Contains a collection of users and groups.
 * 
 * @version $Id: $
 * @since XWiki Core 1.1.2, XWiki Core 1.2M2
 */
public class UsersGroups
{
    /**
     * The collection of users.
     */
    public Collection users = new HashSet();
    
    /**
     * The collection of groups.
     */
    public Collection groups = new HashSet();

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if (!users.isEmpty()) {
            sb.append('{');
            sb.append("users : ");
            sb.append(users);
            sb.append('}');
        }

        if (!groups.isEmpty()) {
            sb.append('{');
            sb.append("groups : ");
            sb.append(groups);
            sb.append('}');
        }

        return sb.toString();
    }
}
