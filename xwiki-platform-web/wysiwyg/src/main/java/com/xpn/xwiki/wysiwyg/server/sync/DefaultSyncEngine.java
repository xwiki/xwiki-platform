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
package com.xpn.xwiki.wysiwyg.server.sync;

import java.util.HashMap;
import java.util.Map;

import com.xpn.xwiki.wysiwyg.client.diff.Diff;
import com.xpn.xwiki.wysiwyg.client.diff.Revision;
import com.xpn.xwiki.wysiwyg.client.diff.ToString;
import com.xpn.xwiki.wysiwyg.client.sync.SyncResult;
import com.xpn.xwiki.wysiwyg.client.sync.SyncStatus;
import com.xpn.xwiki.wysiwyg.client.sync.SyncTools;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DefaultSyncEngine implements SyncEngine
{

    /**
       * Default XWiki logger to report errors correctly.
       */
    private static final Log LOG = LogFactory.getLog(DefaultSyncEngine.class);

    private Map<String, SyncStatus> syncMap = new HashMap<String, SyncStatus>();

    public SyncStatus getSyncStatus(String key)
    {
        return syncMap.get(key);
    }

    public void setSyncStatus(String key, SyncStatus syncStatus)
    {
        syncMap.put(key, syncStatus);
    }

    public SyncResult sync(SyncStatus syncStatus, Revision revision, int version) throws SyncException
    {
        try {
            SyncResult result = new SyncResult();
            String originalContent = syncStatus.getVersion(version);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Current server version is: " + syncStatus.getCurrentVersionNumber());
                LOG.debug("Client version is: " + version);
            }

            if (version == syncStatus.getCurrentVersionNumber()) {
                // this is simple just apply the patch if there is a patch
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Nothing to apply from the server");
                }

                if (revision == null) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Nothing to apply from the client");
                    }
                    return null;
                }

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Applying patch from the client: " + revision);
                    LOG.debug("Original content: " + originalContent);
                }

                String newContent = ToString.arrayToString(revision.patch(ToString.stringToArray(originalContent)));

                if (LOG.isDebugEnabled()) {
                    LOG.debug("New content: " + newContent);
                }
                syncStatus.addVersion(newContent);
                result.setVersion(syncStatus.getCurrentVersionNumber());
                result.setRevision(null);
                result.setStatus(true);
                return result;
            } else {
                String lastContent = syncStatus.getCurrentVersion();
                Revision rev;
                if (lastContent.equals(originalContent))
                    rev = null;
                else {
                    rev = Diff.diff(ToString.stringToArray(originalContent), ToString.stringToArray(lastContent));
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Content on client is based on this content: " + originalContent);
                        LOG.debug("Clients needs to update it's content with rev: " + rev);
                    }
                }

                if (revision == null) {
                    result.setVersion(syncStatus.getCurrentVersionNumber());
                    result.setRevision(rev);
                    result.setStatus(true);
                    return result;
                } else {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Original revision is: " + revision);
                        LOG.debug("Other revision is: " + rev);
                    }
                    if (rev != null) {
                        revision = SyncTools.relocateRevision(revision, rev);
                    }
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Relocated revision is: " + revision);
                        LOG.debug("Content being patched: " + lastContent);
                    }

                    String newContent = ToString.arrayToString(revision.patch(ToString.stringToArray(lastContent)));
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("New content: " + newContent);
                    }

                    // Calculate the new revision to send back
                    Revision newRevision =
                        Diff.diff(ToString.stringToArray(originalContent), ToString.stringToArray(newContent));

                    if (LOG.isDebugEnabled()) {
                        LOG.debug("New revision to apply on the client: " + newRevision);                     
                    }

                    syncStatus.addVersion(newContent);
                    result.setVersion(syncStatus.getCurrentVersionNumber());
                    result.setRevision(newRevision);
                    result.setStatus(true);
                    return result;
                }
            }
        } catch (Exception e) {
            if (LOG.isErrorEnabled())
                LOG.error("Exception while processing sync", e);
            throw new SyncException("Sync Failed", e);

        }

    }
}
