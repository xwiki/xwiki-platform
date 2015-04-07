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
package com.xpn.xwiki.content;

import java.net.URI;

/**
 * Represents the parsed data of a wiki link. The XWiki link format is as follows:
 * <code>(alias[|>])(link)(@interWikiAlias)(|target)</code>, where:
 * <ul>
 *   <li><code>alias</code>: An optional string which will be displayed to the user as the link
 *       name when rendered. Example: "My Page".</li>
 *   <li><code>link</code>: The full link reference using the following syntax:
 *       <code>(virtualWikiAlias:)(space.)(reference)(?queryString)(#anchor)</code>, where:
 *       <ul>
 *         <li><code>virtualWikiAlias</code>: An optional string containing the name of a virtual
 *             wiki. The link will point to a page inside that virtual wiki. Example: "mywiki".</li>
 *         <li><code>space</code>: An optional Wiki Space name. Example: "Main".</li>
 *         <li><code>reference</code>: The link reference. This can be either a URI in the form
 *             <code>protocol:path</code> (example: "http://xwiki.org", "mailto:john@smith.com) or
 *             a wiki page name (example: "WebHome").</li>
 *         <li><code>queryString</code>: An optional query string for specifying parameters that
 *             will be used in the rendered URL. Example: "mydata1=5&mydata2=Hello".</li>
 *         <li><code>anchor</code>: An optional anchor name pointing to an anchor defined in the
 *             referenced link. Note that in XWiki anchors are automatically created for titles.
 *             Example: "TableOfContentAnchor".</li>
 *       </ul>
 *       Either the <code>link</code> or the <code>alias</code> must be specified.</li>
 *   <li><code>interWikiAlias</code>: An optional
 *       <a href="http://en.wikipedia.org/wiki/InterWiki">Inter Wiki</a> alias as defined in the
 *       InterWiki Map. Example: "wikipedia"</li>
 *   <li><code>target</code>: An optional string corresponding to the HTML <code>target</code>
 *       attribute for a <code>a</code> element. This element is used when rendering the link. It
 *       defaults to opening the link in the current page. Example: "_self", "_blank"</li>
 * </ul>
 * Examples of valid wiki links:
 * <ul>
 *   <li>Hello World</li>
 *   <li>Hello World>HelloWorld</li>
 *   <li>Hello World>HelloWorld>_blank</li>
 *   <li>Hello World>http://myserver.com/HelloWorld</li>
 *   <li>Hello World>HelloWorld#Anchor</li>
 *   <li>http://myserver.com</li>
 *   <li>Hello World@Wikipedia</li>
 *   <li>mywiki:HelloWorld</li>
 *   <li>Hello World?param1=1&param2=2</li>
 * </ul>
 *
 * @version $Id$
 */
public class Link implements Cloneable
{
    /**
     * @see #getAlias()
     */
    private String alias;

    /**
     * @see #getVirtualWikiAlias()
     */
    private String virtualWikiAlias;

    /**
     * @see #getSpace()
     */
    private String space;

    /**
     * @see #getPage()
     */
    private String page;

    /**
     * @see #getURI()
     */
    private URI uri;

    /**
     * @see #getQueryString()
     */
    private String queryString;

    /**
     * @see #getAnchor()
     */
    private String anchor;

    /**
     * @see #getInterWikiAlias()
     */
    private String interWikiAlias;

    /**
     * @see #getTarget()
     */
    private String target;

    /**
     * @see #isUsingPipeDelimiter()
     */
    private boolean isUsingPipeDelimiter;

    /**
     * @param alias see {@link #getAlias()}
     */
    public void setAlias(String alias)
    {
        this.alias = alias;
    }

    /**
     * @return the string which will be displayed to the user when the link is rendered or null if no alias has been
     *         specified (in that case the page name or the URI will be displayed. Example: "My Page"
     */
    public String getAlias()
    {
        return this.alias;
    }

    /**
     * @param target see {@link #getTarget()}
     */
    public void setTarget(String target)
    {
        this.target = target;
    }

    /**
     * @return the browser window in which the link should be opened into or null if not defined. This element
     *         corresponds to the HTML <code>target</code> attribute for the <code>a</code> element. It is used when
     *         rendering the link and defauts to opening the link in the current page. Example: "_self", "_blank".
     */
    public String getTarget()
    {
        return this.target;
    }

    /**
     * @param virtualWikiAlias see {@link #getVirtualWikiAlias()}
     */
    public void setVirtualWikiAlias(String virtualWikiAlias)
    {
        this.virtualWikiAlias = virtualWikiAlias;
    }

    /**
     * @return the name of the virtual wiki to which the link is pointing to or null if the link is pointing to the
     *         current wiki. Example: "mywiki"
     */
    public String getVirtualWikiAlias()
    {
        return this.virtualWikiAlias;
    }

    /**
     * @param space see {@link #getSpace()}
     */
    public void setSpace(String space)
    {
        this.space = space;
    }

    /**
     * @return the wiki Space name in which the link points to or null if not defined (in that case the link points to
     *         the current space). Example: "Main"
     */
    public String getSpace()
    {
        return this.space;
    }

    /**
     * @param interWikiAlias see {@link #getInterWikiAlias()}
     */
    public void setInterWikiAlias(String interWikiAlias)
    {
        this.interWikiAlias = interWikiAlias;
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
     * @param anchor see {@link #getAnchor()}
     */
    public void setAnchor(String anchor)
    {
        this.anchor = anchor;
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
     * @param queryString see {@link #getQueryString()}
     */
    public void setQueryString(String queryString)
    {
        this.queryString = queryString;
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
     * @param page see {@link #getPage()}
     */
    public void setPage(String page)
    {
        this.page = page;
    }

    /**
     * @return the Wiki page pointed to by this link or null if the link is pointing to an external URI. Example:
     *         "WebHome".
     */
    public String getPage()
    {
        return this.page;
    }

    /**
     * @param uri see {@link #getURI()}
     */
    public void setURI(URI uri)
    {
        this.uri = uri;
    }

    /**
     * @return the URI this link is pointing to. Valid URIs are mailto links (Example: "mailto:john@smith.com") or URL
     *         (Example: "http://www.xwiki.org").
     */
    public URI getURI()
    {
        return this.uri;
    }

    /**
     * @return true if the link is using the Pipe symbol ("|") as its separator between alias, target and link name, or
     *         false if it's using the greater than symbol (">")
     */
    public boolean isUsingPipeDelimiter()
    {
        return this.isUsingPipeDelimiter;
    }

    /**
     * @param isUsingPipeDelimiter see {@link #isUsingPipeDelimiter()}
     */
    public void setUsePipeDelimiterSymbol(boolean isUsingPipeDelimiter)
    {
        this.isUsingPipeDelimiter = isUsingPipeDelimiter;
    }

    /**
     * @return a String representation of the link without alias nor target. Example: "Space.WebHome#anchor?param1=1"
     */
    private String getLinkName()
    {
        StringBuilder buffer = new StringBuilder();

        if (getVirtualWikiAlias() != null) {
            buffer.append(getVirtualWikiAlias());
            buffer.append(':');
        }

        if (getSpace() != null) {
            buffer.append(getSpace());
            buffer.append('.');
        }

        if (getPage() != null) {
            buffer.append(getPage());
        } else if (getURI() != null) {
            buffer.append(getURI().toString());
        }

        if (getAnchor() != null) {
            buffer.append('#');
            buffer.append(getAnchor());
        }

        if (getQueryString() != null) {
            buffer.append('?');
            buffer.append(getQueryString());
        }

        if (getInterWikiAlias() != null) {
            buffer.append('@');
            buffer.append(getInterWikiAlias());
        }

        return buffer.toString();
    }

    /**
     * Append a delimiter symbol. See {@link #isUsingPipeDelimiter()}
     *
     * @param buffer the buffer to append to
     */
    private void appendDelimiterSymbol(StringBuilder buffer)
    {
        if (isUsingPipeDelimiter()) {
            buffer.append('|');
        } else {
            buffer.append('>');
        }
    }

    @Override
    public String toString()
    {
        StringBuilder buffer = new StringBuilder();

        if (getAlias() != null) {
            buffer.append(getAlias());
            appendDelimiterSymbol(buffer);
        }

        buffer.append(getLinkName());

        if (getTarget() != null) {
            appendDelimiterSymbol(buffer);
            buffer.append(getTarget());
        }

        return buffer.toString();
    }

    /**
     * @return true if the link is external (ie it's not a link on the current local wiki) or false otherwise
     */
    public boolean isExternal()
    {
        return (getURI() != null) || (getInterWikiAlias() != null);
    }

    /**
     * Perform a series of normalization steps on the link. The steps are:
     * <ul>
     *   <li>if the link is not a URI and it doesn't have a page defined then make it point to WebHome</li>
     *   <li>if the link is internal and doesn't have a space defined, fill it in with the current document's space name
     *   </li>
     * </ul>
     *
     * @param currentSpace the space to use when no space has been defined in the link
     * @return the normalized link
     */
    public Link getNormalizedLink(String currentSpace)
    {
        Link normalizedLink;
        try {
            normalizedLink = (Link) clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Failed to clone object [" + this.toString() + "]");
        }

        // If no page was specified, use WebHome
        if ((normalizedLink.getURI() == null) && (normalizedLink.getPage() == null)) {
            normalizedLink.setPage("WebHome");
        }

        // If no space was specified, use the current space
        if (!normalizedLink.isExternal() && (normalizedLink.getSpace() == null)) {
            normalizedLink.setSpace(currentSpace);
        }

        return normalizedLink;
    }
}
