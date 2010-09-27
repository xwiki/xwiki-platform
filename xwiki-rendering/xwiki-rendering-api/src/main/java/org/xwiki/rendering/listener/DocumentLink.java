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
 * Represents a link reference to a Document.
 *
 * @version $Id$
 * @since 2.5M2
 */
public class DocumentLink extends Link
{
    /**
     * @see #getQueryString()
     */
    private String queryString;

    /**
     * @see #getAnchor()
     */
    private String anchor;

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
     * {@inheritDoc}
     * @see Object#toString()
     */
    @Override
    public String toString()
    {
        StringBuffer sb = new StringBuffer(super.toString());
        if (getQueryString() != null) {
            sb.append(" ");
            sb.append("QueryString = [").append(getQueryString()).append("]");
        }
        if (getAnchor() != null) {
            sb.append(" ");
            sb.append("Anchor = [").append(getAnchor()).append("]");
        }
        return sb.toString();        
    }
}
