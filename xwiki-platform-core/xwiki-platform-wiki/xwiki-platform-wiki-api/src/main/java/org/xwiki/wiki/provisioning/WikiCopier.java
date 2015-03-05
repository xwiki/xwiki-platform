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
package org.xwiki.wiki.provisioning;

import org.xwiki.component.annotation.Role;
import org.xwiki.wiki.manager.WikiManagerException;

/**
 * Copy all the documents from a wiki to an other.
 * @since 7.0M2
 * @version $Id :$
 */
@Role
public interface WikiCopier
{
    /**
     * Copy all documents from a wiki to an other.
     * @param fromWikiId Id of the wiki where the documents are located
     * @param toWikiId Id of the wiki that will hold the duplicated documents
     * @param withHistory whether or not the history of the documents should be cloned
     * @throws WikiManagerException if problems occur
     */
    void copyDocuments(String fromWikiId, String toWikiId, boolean withHistory) throws WikiManagerException;

    /**
     * Copy all deleted documents form a wiki to an other.
     * @param fromWikiId Id of the wiki where the documents are located
     * @param toWikiId Id of the wiki that will hold the duplicated documents
     * @throws WikiManagerException if problems occur
     */
    void copyDeletedDocuments(String fromWikiId, String toWikiId) throws WikiManagerException;
}
