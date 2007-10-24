package com.xpn.xwiki.plugin.rightsmanager.utils;

/**
 * Contains all rights for a level right.
 * 
 * @version $Id: $
 */
public class LevelTree
{
    /**
     * List of allow and deny rights for this right level.
     */
    public AllowDeny inherited;

    /**
     * List of inherited allow and deny rights for this right level.
     */
    public AllowDeny direct;

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if (inherited != null) {
            String heritedString = inherited.toString();
            if (heritedString.length() > 0) {
                sb.append('{');
                sb.append("inherited : ");
                sb.append(inherited);
                sb.append('}');
            }
        }

        if (direct != null) {
            String directString = direct.toString();
            if (directString.length() > 0) {
                sb.append('{');
                sb.append("direct : ");
                sb.append(direct);
                sb.append('}');
            }
        }

        return sb.toString();
    }
}
