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

import java.util.Collection;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;
import org.xwiki.wiki.WikiDescriptor;

/**
 * Component to create and manage wikis.
 *
 * @version $Id$
 * @since 5.3M1
 */
@Role
@Unstable
public interface WikiManager
{
    /**
     * Create a new wiki.
     *
     * @param wikiId Id of the new wiki
     * @param wikiAlias Default alias of the new wiki
     * @return the descriptor of the created wiki
     * @throws WikiManagerException if problems occur
     */
    WikiDescriptor create(String wikiId, String wikiAlias) throws WikiManagerException;

    /**
     * Delete a wiki.
     *
     * @param wikiId Id of the wiki to delete.
     * @throws WikiManagerException if problems occur
     */
    void delete(String wikiId) throws WikiManagerException;

    /**
     * Get a wiki from one of its alias.
     *
     * @param wikiAlias Alias of the wiki to retrieve
     * @return The corresponding wiki descriptor of that alias.
     * @throws WikiManagerException if problems occur
     */
    WikiDescriptor getByAlias(String wikiAlias) throws WikiManagerException;

    /**
     * Get a wiki from its Id.
     *
     * @param wikiId Id of the wiki to retrieve.
     * @return The corresponding wiki descriptor of that Id
     * @throws WikiManagerException if problems occur
     */
    WikiDescriptor getById(String wikiId) throws WikiManagerException;

    /**
     * Get the list of all wikis (except the main one).
     *
     * @return the lit of every wiki created on the farm (except the main one).
     * @throws WikiManagerException if problems occur
     */
    Collection<WikiDescriptor> getAll() throws WikiManagerException;

    /**
     * Check if a wiki corresponding to an Id exists.
     *
     * @param wikiId The id of the wiki to test.
     * @return true if a wiki with that Id exists.
     * @throws WikiManagerException if problems occur
     */
    boolean exists(String wikiId) throws WikiManagerException;

    /**
     * Check if the wikiId is valid and available (the name is not already taken for technical reasons).
     *
     * @param wikiId the Id to test
     * @return true if the Id is valid and available
     * @throws WikiManagerException if problems occur
     */
    boolean idAvailable(String wikiId) throws WikiManagerException;

    /**
     * @return the descriptor of the main wiki
     */
    WikiDescriptor getMainWikiDescriptor() throws WikiManagerException;

    /**
     * @return the Id of the main wiki
     */
    String getMainWikiId();
}