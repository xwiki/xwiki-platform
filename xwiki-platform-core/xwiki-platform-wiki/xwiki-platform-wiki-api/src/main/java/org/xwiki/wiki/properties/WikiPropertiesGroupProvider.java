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
package org.xwiki.wiki.properties;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Role;

/**
 * Provider that manager WikiDescriptor Properties Groups.
 *
 * @version $Id$
 * @since 5.3M1
 */
@Component
@Role
public interface WikiPropertiesGroupProvider
{
    /**
     * Get the WikiDescriptor Properties Group corresponding to a wiki.
     *
     * @param wikiId Id of the wiki
     * @return the group corresponding to the wiki
     * @throws WikiPropertiesGroupException if problem occurs
     */
    WikiPropertiesGroup getPropertiesGroup(String wikiId) throws WikiPropertiesGroupException;

    /**
     * Save the WikiDescriptor Properties Group corresponding to a wiki in the persistent storage.
     *
     * @param group group to save
     * @param wikiId uniquement identifier of the wiki concerned by this group
     * @throws WikiPropertiesGroupException if problem occurs
     */
    void savePropertiesGroup(WikiPropertiesGroup group, String wikiId) throws WikiPropertiesGroupException;
}
