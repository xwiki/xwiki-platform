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
package org.xwiki.wiki.manager;

import org.xwiki.component.annotation.Role;
import org.xwiki.wiki.descriptor.WikiDescriptor;

/**
 * Component to create and manage wikis.
 *
 * @version $Id$
 * @since 5.3M2
 */
@Role
public interface WikiManager
{
    /**
     * Create a new wiki.
     *
     * @param wikiId Id of the new wiki
     * @param wikiAlias Default alias of the new wiki
     * @param failOnExist throw an exception if the wikiId already exists
     * @return the descriptor of the created wiki
     * @throws WikiManagerException if problems occur
     */
    WikiDescriptor create(String wikiId, String wikiAlias, boolean failOnExist) throws WikiManagerException;

    /**
     * Copy a wiki.
     *
     * @param fromWikiId If of the wiki to copy
     * @param newWikiId Id of the new wiki
     * @param newWikiAlias Default alias of the new wiki
     * @param copyHistory decide if you want to copy the pages' history
     * @param copyRecycleBin decide if you want to copy the recycle bin content
     * @param failOnExist throw an exception if the wikiId already exists
     * @return the descriptor of the created wiki
     * @throws WikiManagerException if problems occur
     */
    WikiDescriptor copy(String fromWikiId, String newWikiId, String newWikiAlias, boolean copyHistory,
            boolean copyRecycleBin, boolean failOnExist) throws WikiManagerException;

    /**
     * Rename a wiki.
     *
     * @param wikiId If of the wiki to rename
     * @param newWikiId new Id of the wiki
     * @return the descriptor of the renamed wiki
     * @throws WikiManagerException if problems occur
     */
    WikiDescriptor rename(String wikiId, String newWikiId) throws WikiManagerException;

    /**
     * Delete a wiki.
     *
     * @param wikiId Id of the wiki to delete.
     * @throws WikiManagerException if problems occur
     */
    void delete(String wikiId) throws WikiManagerException;

    /**
     * Check if the wikiId is valid and available (the name is not already taken for technical reasons).
     *
     * @param wikiId the Id to test
     * @return true if the Id is valid and available
     * @throws WikiManagerException if problems occur
     */
    boolean idAvailable(String wikiId) throws WikiManagerException;
}
