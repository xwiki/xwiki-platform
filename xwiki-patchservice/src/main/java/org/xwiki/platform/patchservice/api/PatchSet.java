package org.xwiki.platform.patchservice.api;

import java.util.List;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * A <tt>PatchSet</tt> groups several patches together, for easier transfer between hosts. A
 * patchset must contain patches affecting only one document.
 * 
 * @see RWPatchSet
 * @version $Id$
 * @since XWikiPlatform 1.3
 */
public interface PatchSet extends XmlSerializable
{
    /**
     * Get the list of patches included in this set.
     * 
     * @return The list of patches.
     */
    List<Patch> getPatches();

    /**
     * Apply this patch set on a document.
     * 
     * @param doc The document being patched.
     * @param context The XWiki context, needed for some document operations.
     * @throws XWikiException If the patchset cannot be applied on the document.
     */
    void apply(XWikiDocument doc, XWikiContext context) throws XWikiException;
}
