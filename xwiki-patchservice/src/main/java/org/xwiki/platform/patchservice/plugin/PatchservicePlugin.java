package org.xwiki.platform.patchservice.plugin;

import java.security.Key;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

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
    private PatchStorage storage;

    private PatchCreator creator;

    public PatchservicePlugin(String name, String className, XWikiContext context)
    {
        super(name, className, context);
        setClassName(className);
        setName(name);
        try {
            this.storage = new PatchStorage(context);
            this.creator = new org.xwiki.platform.patchservice.hook.PatchCreator(this);
        } catch (XWikiException e) {
            e.printStackTrace();
            // TODO Cannot start storage
        }
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
        System.err.println("patchservice init");
        // TODO Auto-generated method stub
        super.init(context);
        creator.init(context);
        System.err.println("patchservice init done");
    }

    protected PatchStorage getStorage()
    {
        return storage;
    }

    /** Called by XWiki engine for logging a patch. */
    public void logPatch(Patch p)
    {
        getStorage().storePatch(p);
    }

    /** Called by XWiki engine for generating a patch from two document versions. */
    public Patch generatePatch(XWikiDocument oldDoc, XWikiDocument newDoc, XWikiContext context)
    {
        return creator.getPatch(oldDoc, newDoc, context);
    }

    /** Returns a specific patch (if found in the database) or <code>null</code>. */
    public Patch getPatch(PatchId id)
    {
        return getStorage().loadPatch(id);
    }

    /**
     * Retrieves the set of patches that occurred on this host after the patch "from" was applied.
     * This method is inclusive: the from Patch is in the returned list.
     */
    public List<Patch> getUpdatesFrom(PatchId from)
    {
        return getStorage().loadAllPatchesSince(from);
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
     * a patch can be signed using a key, that is registered beforehand to the service: see the
     * registerKey method. The Result object contains information about what happened after the
     * apply tentative.
     */
    public boolean applyPatch(Patch p, PatchId latestPatch, XWikiContext context)
    {
        try {
            p.apply(context.getWiki().getDocument(p.getId().getDocumentId(), context), context);
        } catch (Exception ex) {
            // log.warn(ex.getMessage());
            // TODO is return false enough?
            return false;
        }
        return true;
    }

    /**
     * registers a key in the key ring so that the service can verify that the patch it receives are
     * signed by a known key.
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
