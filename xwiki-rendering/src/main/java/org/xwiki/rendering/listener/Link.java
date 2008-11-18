/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.rendering.listener;

/**
 * Represents a link. Note that this representation is independent of any wiki syntax.
 * 
 * @version $Id: Link.java 10608 2008-06-23 12:25:23Z vmassol $
 * @since 1.5M2
 */
public class Link implements Cloneable
{
    /**
     * @see #getInterWikiAlias()
     */
    private String interWikiAlias;

    /**
     * @see #getReference()
     */
    private String reference;

    /**
     * @see #getType()
     */
    private LinkType type;

    /**
     * @see #getQueryString()
     */
    private String queryString;

    /**
     * @see #getAnchor()
     */
    private String anchor;

    /**
     * @param reference see {@link #getReference()}
     */
    public void setReference(String reference)
    {
        this.reference = reference;
    }

    /**
     * @return the reference pointed to by this link. For example a reference can be a document's name (which depends on
     *         the wiki, for example for XWiki the format is "wiki:space.page"), a URI (for example: mailto:john@doe.com
     *         or a URL) or an <a href="http://en.wikipedia.org/wiki/InterWiki">Inter Wiki</a> reference (which is
     *         appended to the interwiki alias when it's resolved).
     * @see #getType()
     */
    public String getReference()
    {
        return this.reference;
    }

    public LinkType getType()
    {
        return this.type;
    }

    public void setType(LinkType type)
    {
        this.type = type;
    }

    /**
     * @return the <a href="http://en.wikipedia.org/wiki/InterWiki">Inter Wiki</a> alias to which the link is pointing
     *         to or null if not defined. Mappings between Inter Wiki aliases and actual locations are defined in the
     *         Inter Wiki Map. Example: "wikipedia"
     */
    public String getInterWikiAlias()
    {
        return this.interWikiAlias;
    }

    /**
     * @param interWikiAlias see {@link #getInterWikiAlias()}
     */
    public void setInterWikiAlias(String interWikiAlias)
    {
        this.interWikiAlias = interWikiAlias;
    }

    /**
     * @return the query string for specifying parameters that will be used in the rendered URL or null if no query
     *         string has been specified. Example: "mydata1=5&mydata2=Hello"
     */
    public String getQueryString()
    {
        return this.queryString;
    }

    /**
     * @param queryString see {@link #getQueryString()}
     */
    public void setQueryString(String queryString)
    {
        this.queryString = queryString;
    }

    /**
     * @return the anchor name pointing to an anchor defined in the referenced link or null if no anchor has been
     *         specified (in which case the link points to the top of the page). Note that in XWiki anchors are
     *         automatically created for titles. Example: "TableOfContentAnchor"
     */
    public String getAnchor()
    {
        return this.anchor;
    }

    /**
     * @param anchor see {@link #getAnchor()}
     */
    public void setAnchor(String anchor)
    {
        this.anchor = anchor;
    }

    /**
     * @return true if the link points to an external link (ie if it's a URI or an Interwiki link) or false otherwise
     */
    public boolean isExternalLink()
    {
        return (getType() == LinkType.INTERWIKI) || (getType() == LinkType.URI);
    }

    /**
     * The output is syntax independent since this class is used for all syntaxes. Specific syntaxes should extend this
     * class and override this method to perform syntax-dependent formatting.
     */
    @Override
    public String toString()
    {
        boolean shouldAddSpace = false;
        StringBuffer sb = new StringBuffer();
        if (getReference() != null) {
            sb.append(shouldAddSpace ? " " : "");
            sb.append("Reference = [").append(getReference()).append("]");
            shouldAddSpace = true;
        }
        if (getQueryString() != null) {
            sb.append(shouldAddSpace ? " " : "");
            sb.append("QueryString = [").append(getQueryString()).append("]");
            shouldAddSpace = true;
        }
        if (getAnchor() != null) {
            sb.append(shouldAddSpace ? " " : "");
            sb.append("Anchor = [").append(getAnchor()).append("]");
            shouldAddSpace = true;
        }
        if (getInterWikiAlias() != null) {
            sb.append(shouldAddSpace ? " " : "");
            sb.append("InterWikiAlias = [").append(getInterWikiAlias()).append("]");
            shouldAddSpace = true;
        }

        return sb.toString();
    }
    
    /**
     * {@inheritDoc}
     * @see Object#clone()
     */
    @Override
    public Link clone()
    {
        Link clone;
        try {
            clone = (Link) super.clone();
        } catch (CloneNotSupportedException e) {
            // Should never happen
            throw new RuntimeException("Failed to clone object", e);
        }
        return clone;
    }
}
