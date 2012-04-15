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
package org.xwiki.wysiwyg.server.plugin.sync;

import org.xwiki.component.annotation.Role;
import org.xwiki.gwt.wysiwyg.client.diff.Revision;
import org.xwiki.gwt.wysiwyg.client.plugin.sync.SyncResult;
import org.xwiki.gwt.wysiwyg.client.plugin.sync.SyncStatus;

/**
 * This class is responsible for synchronizing the editors when the real-time editing is enabled.
 * 
 * @version $Id$
 */
@Role
public interface SyncEngine
{
    /**
     * @param key a synchronization key; this can be for instance the full name of the edited page
     * @return the status of the given key; this can be for instance the latest version of the edited page
     */
    SyncStatus getSyncStatus(String key);

    /**
     * Sets the status of a synchronization key. It can be used for instance to update the latest version of the edited
     * content.
     * 
     * @param key the synchronization key
     * @param syncStatus the new status for the given key
     */
    void setSyncStatus(String key, SyncStatus syncStatus);

    /**
     * Commits the given revision and updates the status.
     * 
     * @param syncStatus the latest version of the content
     * @param revision the revision to be committed
     * @param version the revision version; specifies what version is affected by the given revision
     * @return the result of the synchronization
     * @throws SyncException if the synchronization fails
     */
    SyncResult sync(SyncStatus syncStatus, Revision revision, int version) throws SyncException;
}
