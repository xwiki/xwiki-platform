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
 * Decides what strategy to use when replacing wiki links. Namely decides when two links are
 * equal and what the replacement link will be. This is because there can different strategy when
 * comparing links: for example are 2 links the same if they have different aliases or different
 * query strings?
 *
 * @version $Id$
 */
public interface ReplaceLinkHandler
{
    /**
     * Compare 2 links.
     *
     * @param linkToLookFor the link to look for
     * @param linkToReplace the link to replace
     * @return true if the 2 links are the same of false otherwise
     */
    boolean compare(Link linkToLookFor, Link linkToReplace);

    /**
     * Compute the link that will be used to replace matching links in the parsed content. This is
     * useful for example to implement a strategy where you only want to replace the space and page
     * names but keep the existing alias, target, query string, etc. 
     * 
     * @param newLink the link to use to replace the matched link. It's possible that only some
     *        fields are set and the replacement algorithm will compute what the others fields
     *        should be
     * @param linkToReplace the link to be replaced
     * @return the link to use for replacing the linkToReplace link
     */
    Link getReplacementLink(Link newLink, Link linkToReplace);
}
