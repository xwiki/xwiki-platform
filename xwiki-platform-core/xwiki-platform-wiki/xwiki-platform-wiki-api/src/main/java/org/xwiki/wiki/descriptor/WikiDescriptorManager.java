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
package org.xwiki.wiki.descriptor;

import java.util.Collection;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.wiki.manager.WikiManagerException;

/**
 * Component to list and get wiki descriptors.
 *
 * @version $Id$
 * @since 5.3M2
 */
@Role
public interface WikiDescriptorManager
{
    /**
     * Get the list of all wikis descriptors.
     *
     * @return the list of every wiki created on the farm
     * @throws org.xwiki.wiki.manager.WikiManagerException if problems occur
     */
    Collection<WikiDescriptor> getAll() throws WikiManagerException;

    /**
     * Get the list of all wikis identifiers.
     *
     * @return the list of every wiki created on the farm
     * @throws org.xwiki.wiki.manager.WikiManagerException if problems occur
     * @since 6.2M1
     */
    Collection<String> getAllIds() throws WikiManagerException;

    /**
     * Get a wiki from one of its aliases.
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
     * @return The corresponding wiki descriptor of that Id, null if none exist for this id
     * @throws WikiManagerException if problems occur
     */
    WikiDescriptor getById(String wikiId) throws WikiManagerException;

    /**
     * @return the descriptor of the main wiki
     * @throws WikiManagerException if problems occur
     */
    WikiDescriptor getMainWikiDescriptor() throws WikiManagerException;

    /**
     * @return the Id of the main wiki
     */
    String getMainWikiId();

    /**
     * @return the Id of the current wiki
     */
    String getCurrentWikiId();

    /**
     * @return the reference of the current wiki.
     * @since 12.7RC1
     */
    default WikiReference getCurrentWikiReference()
    {
        return (getCurrentWikiId() != null) ? new WikiReference(getCurrentWikiId()) : null;
    }

    /**
     * @return the descriptor of the current wiki
     * @throws WikiManagerException if problems occur
     */
    WikiDescriptor getCurrentWikiDescriptor() throws WikiManagerException;

    /**
     * Check if a wiki corresponding to an Id exists.
     *
     * @param wikiId The id of the wiki to test.
     * @return true if a wiki with that Id exists.
     * @throws WikiManagerException if problems occur
     */
    boolean exists(String wikiId) throws WikiManagerException;

    /**
     * Save the given descriptor and all its property groups.
     * 
     * @param descriptor descriptor to save
     * @throws WikiManagerException if problem occurs
     */
    void saveDescriptor(WikiDescriptor descriptor) throws WikiManagerException;

    /**
     * @param wikiId the identifier of the wiki
     * @return true if the passed wiki reference is the main wiki
     * @since 10.4RC1
     */
    default boolean isMainWiki(String wikiId)
    {
        // At least that way it will behave correctly in a single wiki setup. This default probably won't really be
        // needed anyway.
        return true;
    }
}
