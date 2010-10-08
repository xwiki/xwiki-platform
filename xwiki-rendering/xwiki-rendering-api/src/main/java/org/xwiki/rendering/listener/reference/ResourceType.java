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

/**
 * The Resource type. It can be one of:
 * <ul>
 * <li>document ("doc")</li>
 * <li>URL ("url")</li>
 * <li>document in another wiki (interwiki) ("interwiki")</li>
 * <li>relative URL ("path")</li>
 * <li>mail ("mailto")</li>
 * <li>attachment ("attach")</li>
 * <li>image ("image")</li>
 * </ul>
 * 
 * @version $Id$
 * @since 2.5RC1
 */
public class ResourceType
{
    /**
     * Represents a Document.
     */
    public static final ResourceType DOCUMENT = new ResourceType("doc");

    /**
     * Represents an URL.
     */
    public static final ResourceType URL = new ResourceType("url");

    /**
     * Represents a document in another wiki.
     */
    public static final ResourceType INTERWIKI = new ResourceType("interwiki");
    
    /**
     * Represents a relative URL in the current wiki.
     */
    public static final ResourceType PATH = new ResourceType("path");

    /**
     * Represents a mail.
     */
    public static final ResourceType MAILTO = new ResourceType("mailto");

    /**
     * Represents an attachment.
     */
    public static final ResourceType ATTACHMENT = new ResourceType("attach");

    /**
     * Represents an image.
     */
    public static final ResourceType IMAGE = new ResourceType("image");

    /**
     * @see #getScheme()
     */
    private String scheme;

    /**
     * @param scheme see {@link #getScheme()}
     */
    public ResourceType(String scheme)
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
                // object must be ResourceType at this point
                ResourceType type = (ResourceType) object;
                result = (getScheme() == type.getScheme() || (getScheme() != null
                    && getScheme().equals(type.getScheme())));
            }
        }
        return result;
    }
}
