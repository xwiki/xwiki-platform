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
package org.xwiki.wiki.internal.manager;

import org.xwiki.component.annotation.Role;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.manager.WikiManagerException;

/**
 * Component to create wiki.
 *
 * @version $Id$
 */
@Role
public interface WikiCreator
{
    /**
     * Create a new empty wiki.
     *
     * @param wikiId id of the wiki to create
     * @param wikiAlias default alias of the wiki to create
     * @return the descriptor of the new wiki
     * @throws WikiManagerException if problems occur
     */
    WikiDescriptor create(String wikiId, String wikiAlias) throws WikiManagerException;

    /**
     * Create a new empty wiki.
     *
     * @param wikiId id of the wiki to create
     * @param wikiAlias default alias of the wiki to create
     * @param ownerId the identifier of the owner of the wiki
     * @return the descriptor of the new wiki
     * @throws WikiManagerException if problems occur
     * @since 11.3
     * @since 10.11.8
     */
    WikiDescriptor create(String wikiId, String wikiAlias, String ownerId) throws WikiManagerException;
}
