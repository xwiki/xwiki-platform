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

public class DefaultSyncEngine implements SyncEngine
{
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
            if (version == syncStatus.getCurrentVersionNumber()) {
                // this is simple just apply the patch if there is a patch
                if (revision == null)
                    return null;

                String newContent = ToString.arrayToString(revision.patch(ToString.stringToArray(originalContent)));
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
                else
                    rev = Diff.diff(ToString.stringToArray(originalContent), ToString.stringToArray(lastContent));

                if (revision == null) {
                    result.setVersion(syncStatus.getCurrentVersionNumber());
                    result.setRevision(rev);
                    result.setStatus(true);
                    return result;
                } else {
                    if (rev != null) {
                        revision = SyncTools.relocateRevision(revision, rev);
                    }
                    String newContent = ToString.arrayToString(revision.patch(ToString.stringToArray(lastContent)));
                    // Calculate the new revision to send back
                    Revision newRevision =
                        Diff.diff(ToString.stringToArray(originalContent), ToString.stringToArray(newContent));
                    syncStatus.addVersion(newContent);
                    result.setVersion(syncStatus.getCurrentVersionNumber());
                    result.setRevision(newRevision);
                    result.setStatus(true);
                    return result;
                }
            }
        } catch (Exception e) {
            throw new SyncException("Sync Failed", e);
        }

    }
}
