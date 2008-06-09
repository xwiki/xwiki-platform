package org.xwiki.platform.patchservice.api;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * The <tt>PatchCreator</tt> is a helper class, which generates {@link Patch patches} from two
 * versions of a document, by comparing the document versions and splitting the differences into
 * basic {@link Operation operations}.
 * 
 * @version $Id$
 * @since XWikiPlatform 1.3
 */
public interface PatchCreator
{
    void init(XWikiContext context);
    /**
     * Create a {@link Patch} which, when applied on oldDoc, will result in newDoc.
     * 
     * @param oldDoc The initial version of the document.
     * @param newDoc The updated version of the document.
     * @return A {@link Patch patch} reflecting the changes between the two versions. 
     */
    Patch getPatch(XWikiDocument oldDoc, XWikiDocument newDoc, XWikiContext context);
}
