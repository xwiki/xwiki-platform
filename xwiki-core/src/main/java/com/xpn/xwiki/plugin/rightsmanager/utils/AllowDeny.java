package com.xpn.xwiki.plugin.rightsmanager.utils;

/**
 * Contains list of users and groups for allow right and deny right.
 * 
 * @version $Id$
 * @since XWiki Core 1.1.2, XWiki Core 1.2M2
 */
public class AllowDeny
{
    /**
     * List of users and groups for allow right.
     */
    public UsersGroups allow = new UsersGroups();

    /**
     * List of users and group for deny right.
     */
    public UsersGroups deny = new UsersGroups();

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        String allowString = this.allow.toString();
        if (allowString.length() > 0) {
            sb.append('{');
            sb.append("allow : ");
            sb.append(this.allow);
            sb.append('}');
        }

        String denyString = this.deny.toString();
        if (denyString.length() > 0) {
            sb.append('{');
            sb.append("deny : ");
            sb.append(this.deny);
            sb.append('}');
        }

        return sb.toString();
    }
}
