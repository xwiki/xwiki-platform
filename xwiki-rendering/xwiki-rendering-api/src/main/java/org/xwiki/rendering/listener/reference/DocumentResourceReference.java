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
package org.xwiki.rendering.listener.reference;

import org.apache.commons.lang.StringUtils;

/**
 * Represents a reference to a Document.
 *
 * @version $Id$
 * @since 2.5RC1
 */
public class DocumentResourceReference extends ResourceReference
{
    /**
     * The name of the parameter representing the Query String.
     */
    public static final String QUERY_STRING = "queryString";

    /**
     * The name of the parameter representing the Anchor.
     */
    public static final String ANCHOR = "anchor";
    
    /**
     * @param reference see {@link #getReference()}
     */
    public DocumentResourceReference(String reference)
    {
        super(reference, ResourceType.DOCUMENT);
    }
    
    /**
     * @return the query string for specifying parameters that will be used in the rendered URL or null if no query
     *         string has been specified. Example: "mydata1=5&mydata2=Hello"
     */
    public String getQueryString()
    {
        return getParameter(QUERY_STRING);
    }

    /**
     * @param queryString see {@link #getQueryString()}
     */
    public void setQueryString(String queryString)
    {
        if (!StringUtils.isEmpty(queryString)) {
            setParameter(QUERY_STRING, queryString);
        }
    }

    /**
     * @return the anchor name pointing to an anchor defined in the referenced document or null if no anchor has been
     *         specified (in which case the reference points to the top of the document). Note that in XWiki anchors are
     *         automatically created for titles. Example: "TableOfContentAnchor"
     */
    public String getAnchor()
    {
        return getParameter(ANCHOR);
    }

    /**
     * @param anchor see {@link #getAnchor()}
     */
    public void setAnchor(String anchor)
    {
        if (!StringUtils.isEmpty(anchor)) {
            setParameter(ANCHOR, anchor);
        }
    }
}
