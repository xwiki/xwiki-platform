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
import java.util.ListIterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xwiki.platform.patchservice.api.Patch;
import org.xwiki.platform.patchservice.api.PatchCreator;
import org.xwiki.platform.patchservice.api.PatchId;
import org.xwiki.platform.patchservice.storage.PatchStorage;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;

public class PatchservicePlugin extends XWikiDefaultPlugin implements XWikiPluginInterface
{
    /** Logging helper object. */
    private static final Log LOG = LogFactory.getLog(PatchservicePlugin.class);

    private PatchStorage storage;

    private PatchCreator creator;

    public PatchservicePlugin(String name, String className, XWikiContext context)
    {
        super(name, className, context);
        setClassName(className);
        setName(name);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#getName()
     */
    @Override
    public String getName()
    {
        return "patchservice";
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#init(com.xpn.xwiki.XWikiContext)
     */
    @Override
    public void init(XWikiContext context)
    {
        super.init(context);
        try {
            this.storage = new PatchStorage(context);
        } catch (XWikiException ex) {
            LOG.error("Cannot initialize patch storage", ex);
        }
        this.creator = new org.xwiki.platform.patchservice.hook.PatchCreator(this);
        this.creator.init(context);
    }

    protected PatchStorage getStorage()
    {
        return this.storage;
    }

    /** Called by XWiki engine for logging a patch. */
    public void logPatch(Patch p)
    {
        getStorage().storePatch(p);
    }

    /** Called by XWiki engine for generating a patch from two document versions. */
    public Patch generatePatch(XWikiDocument oldDoc, XWikiDocument newDoc, XWikiContext context)
    {
        return this.creator.getPatch(oldDoc, newDoc, context);
    }

    /** Returns a specific patch (if found in the database) or <code>null</code>. */
    public Patch getPatch(PatchId id)
    {
        return getStorage().loadPatch(id);
    }

    /**
     * Retrieves the set of patches that occurred on this host after the patch "from" was applied. This method is
     * inclusive: the from Patch is in the returned list.
     */
    public List<Patch> getUpdatesFrom(PatchId from)
    {
        return getStorage().loadAllPatchesSince(from);
    }

    /**
     * Retrieves the set of patches that occurred on this host after the patch "from" was applied. This method is
     * inclusive: the from Patch is in the returned list.
     */
    public List<Patch> getDocumentUpdatesFrom(PatchId from)
    {
        return getStorage().loadAllDocumentPatchesSince(from);
    }

    public List<Patch> getAllPatches()
    {
        return getStorage().loadAllPatches();
    }

    /** This method is inclusive: from and to patches are included */
    public List<Patch> getDelta(PatchId fromPatch, PatchId toPatch)
    {
        List<Patch> patches = getStorage().loadAllDocumentPatchesSince(fromPatch);
        ListIterator<Patch> it = patches.listIterator();
        while (it.hasNext()) {
            if (it.next().getId().equals(toPatch)) {
                break;
            }
        }
        return patches.subList(0, it.nextIndex());
    }

    /** do we need to introduce a concept of dependencies between patches? */
    public Iterator<Patch> getDependencies(PatchId id)
    {
        // TODO write me!
        return null;
    }

    /**
     * a patch can be signed using a key, that is registered beforehand to the service: see the registerKey method. The
     * Result object contains information about what happened after the apply tentative.
     */
    public boolean applyPatch(Patch p, PatchId latestPatch, XWikiContext context)
    {
        try {
            XWikiDocument doc = context.getWiki().getDocument(p.getId().getDocumentId(), context);
            p.apply(doc, context);
            context.getWiki().getStore().saveXWikiDoc(doc, context);
        } catch (Exception ex) {
            LOG.warn(ex.getMessage(), ex);
            // TODO is return false enough?
            return false;
        }
        return true;
    }

    /**
     * registers a key in the key ring so that the service can verify that the patch it receives are signed by a known
     * key.
     */
    public void registerKey(Key k)
    {
        // TODO write me!
    }

    /** unregisters a key */
    public void unregisterKey(Key k)
    {
        // TODO write me!
    }
}
