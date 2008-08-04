package com.xpn.xwiki.plugin.rightsmanager.utils;

/**
 * Contains all rights for a level right.
 * 
 * @version $Id$
 * @since XWiki Core 1.1.2, XWiki Core 1.2M2
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
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if (this.inherited != null) {
            String heritedString = this.inherited.toString();
            if (heritedString.length() > 0) {
                sb.append('{');
                sb.append("inherited : ");
                sb.append(this.inherited);
                sb.append('}');
            }
        }

        if (this.direct != null) {
            String directString = this.direct.toString();
            if (directString.length() > 0) {
                sb.append('{');
                sb.append("direct : ");
                sb.append(this.direct);
                sb.append('}');
            }
        }

        return sb.toString();
    }
}
