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
package org.xwiki.wiki;

import java.util.Collection;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Entry point to manipulate wiki descriptors (add, remove, get).
 *
 * @version $Id$
 * @since 5.3M1
 */
@Role
@Unstable
public interface WikiDescriptorManager
{
    /**
     * @param wikiAlias the alias of the wiki for which to return the descriptor
     * @return the descriptor matching the passed alias from cache or if not in cache then by looking up the data in
     *         a {@code XWikiServer*} wiki page having a {@code XWikiServerClass} XObject and a {@code server} property
     *         matching the passed alias.
     * @throws WikiDescriptorException in case of an error locating wiki pages
     */
    WikiDescriptor getByWikiAlias(String wikiAlias) throws WikiDescriptorException;

    /**
     * @param wikiId the id of the wiki for which to return the descriptor
     * @return the descriptor matching the passed id from cache or if not in cache the by looking up the data in a
     *         a {@code XWikiServer&lt;wikiId&gt;} page
     * @throws WikiDescriptorException in case of an error locating wiki pages
     */
    WikiDescriptor getByWikiId(String wikiId) throws WikiDescriptorException;

    /**
     * Save both the wiki alias and the wiki id contained in the passed descriptor in cache.
     *
     * @param descriptor the descriptor containing the id and alias to save in cache
     */
    void set(WikiDescriptor descriptor);

    /**
     * Remove both the wiki alias and the wiki id contained in the passed descriptor from cache.
     *
     * @param descriptor the descriptor containing the id and alias to remove from cache
     */
    void remove(WikiDescriptor descriptor);

    /**
     * @return all the wiki descriptors found in the current wiki. They are found by looking up all pages named
     *         {@code XWikiServer*} and having a {@code XWikiServerClass} XObject.
     * @throws WikiDescriptorException in case of an error locating wiki pages
     */
    Collection<WikiDescriptor> getAll() throws WikiDescriptorException;
}
