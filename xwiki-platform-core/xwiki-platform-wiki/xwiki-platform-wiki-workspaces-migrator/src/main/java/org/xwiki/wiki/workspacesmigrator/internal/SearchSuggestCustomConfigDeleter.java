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
package org.xwiki.wiki.workspacesmigrator.internal;

import org.xwiki.component.annotation.Role;

import com.xpn.xwiki.XWikiException;

/**
 * Component that remove SearchSuggestConfig objects that was previously added by WorkspaceManager.Install.
 * If we don't remove them, it will cause a conflict in DW since we have added a new object in the standard
 * distribution (see http://jira.xwiki.org/browse/XWIKI-9697).
 *
 * @since 5.3RC1
 * @version $Id$
 */
@Role
public interface SearchSuggestCustomConfigDeleter
{
    /**
     * The WorkspaceManager.Install script has added a new XWiki.SearchSuggestSourceClass object that we need to remove.
     *
     * @param wikiId id of the wiki where the config should be removed
     * @throws XWikiException if problem occurs
     */
    void deleteSearchSuggestCustomConfig(String wikiId) throws XWikiException;
}
