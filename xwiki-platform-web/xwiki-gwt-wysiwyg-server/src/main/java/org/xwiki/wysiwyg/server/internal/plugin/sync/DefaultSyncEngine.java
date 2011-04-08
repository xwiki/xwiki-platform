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
package org.xwiki.wysiwyg.server.internal.plugin.sync;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xwiki.gwt.wysiwyg.client.diff.Diff;
import org.xwiki.gwt.wysiwyg.client.diff.Revision;
import org.xwiki.gwt.wysiwyg.client.diff.ToString;
import org.xwiki.gwt.wysiwyg.client.plugin.sync.SyncResult;
import org.xwiki.gwt.wysiwyg.client.plugin.sync.SyncStatus;
import org.xwiki.gwt.wysiwyg.client.plugin.sync.SyncTools;
import org.xwiki.wysiwyg.server.plugin.sync.SyncEngine;
import org.xwiki.wysiwyg.server.plugin.sync.SyncException;


/**
 * Default implementation of {@link SyncEngine}.
 * 
 * @version $Id$
 */
public class DefaultSyncEngine implements SyncEngine
{
    /**
     * Default XWiki logger to report errors correctly.
     */
    private static final Log LOG = LogFactory.getLog(DefaultSyncEngine.class);

    /**
     * The map of synchronization statuses.
     */
    private Map<String, SyncStatus> syncMap = new HashMap<String, SyncStatus>();

    /**
     * {@inheritDoc}
     * 
     * @see SyncEngine#getSyncStatus(String)
     */
    public SyncStatus getSyncStatus(String key)
    {
        return syncMap.get(key);
    }

    /**
     * {@inheritDoc}
     * 
     * @see SyncEngine#setSyncStatus(String, SyncStatus)
     */
    public void setSyncStatus(String key, SyncStatus syncStatus)
    {
        syncMap.put(key, syncStatus);
    }

    /**
     * {@inheritDoc}
     * 
     * @see SyncEngine#sync(SyncStatus, Revision, int)
     */
    public SyncResult sync(SyncStatus syncStatus, Revision revision, int version) throws SyncException
    {
        try {
            SyncResult result = new SyncResult();
            String originalContent = syncStatus.getVersion(version);

            LOG.debug("Current server version is: " + syncStatus.getCurrentVersionNumber());
            LOG.debug("Client version is: " + version);

            if (version == syncStatus.getCurrentVersionNumber()) {
                // this is simple just apply the patch if there is a patch
                LOG.debug("Nothing to apply from the server");

                if (revision == null) {
                    LOG.debug("Nothing to apply from the client");
                    return null;
                }

                LOG.debug("Applying patch from the client: " + revision);
                LOG.debug("Original content: " + originalContent);

                String newContent = ToString.arrayToString(revision.patch(ToString.stringToArray(originalContent)));

                LOG.debug("New content: " + newContent);
                syncStatus.addVersion(newContent);
                result.setVersion(syncStatus.getCurrentVersionNumber());
                result.setRevision(null);
                result.setStatus(true);
                return result;
            } else {
                String lastContent = syncStatus.getCurrentVersion();
                Revision rev;
                if (lastContent.equals(originalContent)) {
                    rev = null;
                } else {
                    rev = Diff.diff(ToString.stringToArray(originalContent), ToString.stringToArray(lastContent));
                    LOG.debug("Content on client is based on this content: " + originalContent);
                    LOG.debug("Clients needs to update it's content with rev: " + rev);
                }

                if (revision == null) {
                    result.setVersion(syncStatus.getCurrentVersionNumber());
                    result.setRevision(rev);
                    result.setStatus(true);
                    return result;
                } else {
                    LOG.debug("Original revision is: " + revision);
                    LOG.debug("Other revision is: " + rev);
                    if (rev != null) {
                        revision = SyncTools.relocateRevision(revision, rev);
                    }
                    LOG.debug("Relocated revision is: " + revision);
                    LOG.debug("Content being patched: " + lastContent);

                    String newContent = ToString.arrayToString(revision.patch(ToString.stringToArray(lastContent)));
                    LOG.debug("New content is: " + newContent);

                    // Calculate the new revision to send back
                    Revision newRevision =
                        Diff.diff(ToString.stringToArray(originalContent), ToString.stringToArray(newContent));

                    LOG.debug("New revision to apply on the client: " + newRevision);

                    syncStatus.addVersion(newContent);
                    result.setVersion(syncStatus.getCurrentVersionNumber());
                    result.setRevision(newRevision);
                    result.setStatus(true);
                    return result;
                }
            }
        } catch (Exception e) {
            LOG.error("Exception while processing sync", e);
            throw new SyncException("Sync Failed", e);
        }
    }
}
