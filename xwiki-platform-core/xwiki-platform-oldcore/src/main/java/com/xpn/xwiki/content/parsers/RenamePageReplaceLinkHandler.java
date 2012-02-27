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
package com.xpn.xwiki.content.parsers;

import com.xpn.xwiki.content.Link;

/**
 * A replacement link handler used for renaming documents and backlinks pointing to them.
 *
 * @version $Id$
 */
public class RenamePageReplaceLinkHandler implements ReplaceLinkHandler
{
    /**
     * {@inheritDoc}
     *
     * <p>Two links are equal if they point to the same document and within the same location
     * in that document.</p>
     * 
     * @see ReplaceLinkHandler#compare(Link, Link) 
     */
    @Override
    public boolean compare(Link linkToLookFor, Link linkToReplace)
    {
        boolean result;

        if ((linkToLookFor == null) || (linkToReplace == null)) {
            result = false;
        } else {
            result =
                (((linkToLookFor.getAnchor() != null)
                        && linkToLookFor.getAnchor().equals(linkToReplace.getAnchor()))
                    || ((linkToLookFor.getAnchor() == null) && (linkToReplace.getAnchor() == null)))
                && (((linkToLookFor.getInterWikiAlias() != null)
                        && linkToLookFor.getInterWikiAlias().equals(
                            linkToReplace.getInterWikiAlias()))
                    || ((linkToLookFor.getInterWikiAlias() == null)
                        && (linkToReplace.getInterWikiAlias() == null)))
                && (((linkToLookFor.getPage() != null)
                        && linkToLookFor.getPage().equals(linkToReplace.getPage()))
                    || ((linkToLookFor.getPage() == null) && (linkToReplace.getPage() == null)))
                && (((linkToLookFor.getSpace() != null)
                        && linkToLookFor.getSpace().equals(linkToReplace.getSpace()))
                    || ((linkToLookFor.getSpace() == null) && (linkToReplace.getSpace() == null)))
                && (((linkToLookFor.getURI() != null)
                        && linkToLookFor.getURI().equals(linkToReplace.getURI()))
                    || ((linkToLookFor.getURI() == null) && (linkToReplace.getURI() == null)))
                && (((linkToLookFor.getVirtualWikiAlias() != null)
                        && linkToLookFor.getVirtualWikiAlias().equals(
                            linkToReplace.getVirtualWikiAlias()))
                    || ((linkToLookFor.getVirtualWikiAlias() == null)
                        && (linkToReplace.getVirtualWikiAlias() == null)));
        }

        return result;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Keep the query string, alias and target in the link to replace if they are not
     * specified in the new link.</p>
     *
     * @see ReplaceLinkHandler#getReplacementLink(Link, Link)
     */
    @Override
    public Link getReplacementLink(Link newLink, Link linkToReplace)
    {
        Link replacementLink = new Link();

        replacementLink.setPage(newLink.getPage());
        replacementLink.setSpace(newLink.getSpace());
        replacementLink.setAnchor(newLink.getAnchor());
        replacementLink.setInterWikiAlias(newLink.getInterWikiAlias());
        replacementLink.setURI(newLink.getURI());
        replacementLink.setVirtualWikiAlias(newLink.getVirtualWikiAlias());
        replacementLink.setUsePipeDelimiterSymbol(linkToReplace.isUsingPipeDelimiter());

        if (newLink.getAlias() != null) {
            replacementLink.setAlias(newLink.getAlias());
        } else {
            replacementLink.setAlias(linkToReplace.getAlias());
        }

        if (newLink.getTarget() != null) {
            replacementLink.setTarget(newLink.getTarget());
        } else {
            replacementLink.setTarget(linkToReplace.getTarget());
        }

        if (newLink.getQueryString() != null) {
            replacementLink.setQueryString(newLink.getQueryString());
        } else {
            replacementLink.setQueryString(linkToReplace.getQueryString());
        }

        return replacementLink;
    }
}
