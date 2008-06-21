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
 *
 */
package org.xwiki.platform.patchservice.plugin;

import java.security.Key;
import java.util.Iterator;
import java.util.List;

import org.xwiki.platform.patchservice.api.Patch;
import org.xwiki.platform.patchservice.api.PatchId;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.PluginApi;

public class PatchservicePluginApi extends PluginApi<PatchservicePlugin>
{
    public PatchservicePluginApi(PatchservicePlugin plugin, XWikiContext context)
    {
        super(plugin, context);
    }

    /** Called by XWiki engine for logging a patch. */
    public void logPatch(Patch p)
    {
        getProtectedPlugin().logPatch(p);
    }

    /** Called by XWiki engine for generating a patch from two document versions. */
    public Patch generatePatch(XWikiDocument oldDoc, XWikiDocument newDoc, XWikiContext context)
    {
        return getProtectedPlugin().generatePatch(oldDoc, newDoc, context);
    }

    /** Returns a specific patch (if found in the database) or <code>null</code>. */
    public Patch getPatch(PatchId id)
    {
        return getProtectedPlugin().getPatch(id);
    }

    /**
     * Retrieves the set of patches that occurred on this host after the patch "from" was applied. This method is
     * inclusive: the from Patch is in the returned list.
     */
    public List<Patch> getUpdatesFrom(PatchId from)
    {
        return getProtectedPlugin().getUpdatesFrom(from);
    }

    public List<Patch> getAllPatches()
    {
        return getProtectedPlugin().getAllPatches();
    }

    /** This method is inclusive: from and to patches are included */
    public List<Patch> getDelta(PatchId fromPatch, PatchId toPatch)
    {
        return getProtectedPlugin().getDelta(fromPatch, toPatch);
    }

    /** do we need to introduce a concept of dependencies between patches? */
    public Iterator<Patch> getDependencies(PatchId id)
    {
        return getProtectedPlugin().getDependencies(id);
    }

    /**
     * a patch can be signed using a key, that is registered beforehand to the service: see the registerKey method. The
     * Result object contains information about what happened after the apply tentative.
     */
    public boolean applyPatch(Patch p, PatchId latestPatch)
    {
        return getProtectedPlugin().applyPatch(p, latestPatch, getXWikiContext());
    }

    /**
     * registers a key in the key ring so that the service can verify that the patch it receives are signed by a known
     * key.
     */
    public void registerKey(Key k)
    {
        getProtectedPlugin().registerKey(k);
    }

    /** unregisters a key */
    public void unregisterKey(Key k)
    {
        getProtectedPlugin().unregisterKey(k);
    }
}
