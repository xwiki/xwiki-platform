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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
    private boolean isTyped = true;

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
     * @see #getParameter(String) 
     */
    private Map<String, String> parameters = new HashMap<String, String>();

    /**
     * @param isTyped see {@link #isTyped()}
     */
    public void setTyped(boolean isTyped)
    {
        this.isTyped = isTyped;
    }

    /**
     * @return true if the link type has been explicitly provided (eg in XWiki Syntax 2.1 if the reference is prefixed
     *         with the link type followed by ":" and then the rest of the reference)
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
     * @param name see {@link #getParameter(String)}
     * @param value see {@link #getParameter(String)}
     */
    public void setParameter(String name, String value)
    {
        this.parameters.put(name, value);
    }

    /**
     * @param name see {@link #getParameter(String)}
     */
    public void removeParameter(String name)
    {
        this.parameters.remove(name);
    }
    /**
     * In order for Link references to be extensible we allow for extra parameters in addition to the link reference.
     * For example this is used in Document Links for storing the query string and anchor information, and in InterWiki
     * Links to store the InterWiki Alias. Note that supported parameters depend on the Renderer that will be used
     * (i.e. it depends on the target Syntax). For example the XWiki Syntax 2.1 only supports "queryString" and
     * "anchor".
     *
     * @param name the name of the parameter to get
     * @return the parameter value or null if no such parameter exist
     */
    public String getParameter(String name)
    {
        return this.parameters.get(name);
    }

    /**
     * @return the collections of parameters, see {@link #getParameter(String)}
     */
    public Map<String, String> getParameters()
    {
        return Collections.unmodifiableMap(this.parameters);
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
        StringBuffer sb = new StringBuffer();
        sb.append("Typed = [").append(isTyped()).append("]");
        sb.append(" ");
        sb.append("Type = [").append(getType().getScheme()).append("]");
        if (getReference() != null) {
            sb.append(" ");
            sb.append("Reference = [").append(getReference()).append("]");
        }
        Map<String, String> params = getParameters();
        if (!params.isEmpty()) {
            sb.append(" ");
            sb.append("Parameters = [");
            Iterator<Map.Entry<String, String>> it = params.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, String> entry = it.next();
                sb.append("[").append(entry.getKey()).append("] = [").append(entry.getValue()).append("]");
                if (it.hasNext()) {
                    sb.append(", ");
                }
            }
            sb.append("]");
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
