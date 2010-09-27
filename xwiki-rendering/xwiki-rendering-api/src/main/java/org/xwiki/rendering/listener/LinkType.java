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
 * The link type (a link to a document, a link to a mail, a link to an external interwiki, etc).
 * 
 * @version $Id$
 * @since 2.5M2
 */
public class LinkType
{
    /**
     * The link targets a document.
     */
    public static final LinkType DOCUMENT = new LinkType("doc");

    /**
     * The link targets an URL.
     */
    public static final LinkType URL = new LinkType("url");

    /**
     * The link targets a document in another wiki.
     */
    public static final LinkType INTERWIKI = new LinkType("interwiki");
    
    /**
     * The link targets a relative URL in the current wiki.
     */
    public static final LinkType PATH = new LinkType("path");

    /**
     * The link targets a mail.
     */
    public static final LinkType MAILTO = new LinkType("mailto");

    /**
     * The link targets an attachment.
     */
    public static final LinkType ATTACHMENT = new LinkType("attach");

    /**
     * The link targets an image.
     */
    public static final LinkType IMAGE = new LinkType("image");

    /**
     * @see #getScheme()
     */
    private String scheme;

    /**
     * @param scheme see {@link #getScheme()}
     */
    public LinkType(String scheme)
    {
        setScheme(scheme);
    }

    /**
     * @return the type of the link (eg "doc" for links to documents, etc)
     */
    public String getScheme()
    {
        return this.scheme;
    }

    /**
     * @param scheme see {@link #getScheme()}
     */
    public void setScheme(String scheme)
    {
        this.scheme = scheme;
    }

    /**
     * {@inheritDoc}
     *
     * @see Object#hashCode()
     */
    @Override
    public int hashCode()
    {
        // Random number. See http://www.technofundo.com/tech/java/equalhash.html for the detail of this
        // algorithm.
        int hash = 8;
        hash = 31 * hash + (null == getScheme() ? 0 : getScheme().hashCode());
        return hash;
    }

    /**
     * {@inheritDoc}
     *
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals(Object object)
    {
        boolean result;

        // See http://www.technofundo.com/tech/java/equalhash.html for the detail of this algorithm.
        if (this == object) {
            result = true;
        } else {
            if ((object == null) || (object.getClass() != this.getClass())) {
                result = false;
            } else {
                // object must be LinkType at this point
                LinkType linkType = (LinkType) object;
                result = (getScheme() == linkType.getScheme() || (getScheme() != null
                    && getScheme().equals(linkType.getScheme())));
            }
        }
        return result;
    }
}
