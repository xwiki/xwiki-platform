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
 * Represents a reference to a link. Note that this representation is independent of any wiki syntax.
 *
 * @version $Id$
 * @since 1.5M2
 */
public class Link implements Cloneable
{
    /**
     * @see #isTyped()
     */
    private boolean isTyped;

    /**
     * @see #getReference()
     *
     * Note that the reason we store the reference as a String and not as an Entity Reference is because we want
     * the Rendering module independent of the XWiki Model so that it can be used independently of XWiki.
     */
    private String reference;

    /**
     * @see #getType()
     */
    private LinkType type;

    /**
     * @param isTyped see {@link #isTyped()}
     */
    public void setTyped(boolean isTyped)
    {
        this.isTyped = isTyped;
    }

    /**
     * @return true if the link reference is prefixed with the link type (eg "doc" for links to documents, etc)
     */
    public boolean isTyped()
    {
        return this.isTyped;
    }

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
     *         or a URL),n <a href="http://en.wikipedia.org/wiki/InterWiki">Inter Wiki</a> reference, etc
     * @see #getType()
     */
    public String getReference()
    {
        return this.reference;
    }

    /**
     * @return the type of link
     * @see LinkType
     */
    public LinkType getType()
    {
        return this.type;
    }

    /**
     * @param type the type of link
     * @see LinkType
     */
    public void setType(LinkType type)
    {
        this.type = type;
    }

    /**
     * {@inheritDoc} <p> The output is syntax independent since this class is used for all syntaxes. Specific syntaxes
     * should extend this class and override this method to perform syntax-dependent formatting.
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        boolean shouldAddSpace = false;
        StringBuffer sb = new StringBuffer();
        sb.append("Typed = [").append(isTyped()).append("]");
        sb.append(" ");
        sb.append("Type = [").append(getType().getScheme()).append("]");
        shouldAddSpace = true;
        if (getReference() != null) {
            sb.append(shouldAddSpace ? " " : "");
            sb.append("Reference = [").append(getReference()).append("]");
            shouldAddSpace = true;
        }

        return sb.toString();
    }

    /**
     * {@inheritDoc}
     *
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
