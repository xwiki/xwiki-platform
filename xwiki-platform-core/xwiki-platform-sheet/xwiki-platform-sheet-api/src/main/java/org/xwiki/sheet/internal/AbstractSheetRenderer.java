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
package org.xwiki.sheet.internal;

import javax.inject.Inject;

import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.sheet.SheetRenderer;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.user.api.XWikiRightService;

/**
 * Abstract {@link SheetRenderer} implementation that ensures the programming rights level of the sheet is preserved
 * when rendering the sheet in the context of the target document.
 * 
 * @version $Id$
 */
public abstract class AbstractSheetRenderer implements SheetRenderer
{
    /**
     * Execution context handler, needed for accessing the XWikiContext.
     */
    @Inject
    private Execution execution;

    /**
     * @return the XWiki context
     * @deprecated avoid using this method; try using the document access bridge instead
     */
    protected XWikiContext getXWikiContext()
    {
        return (XWikiContext) execution.getContext().getProperty("xwikicontext");
    }

    @Override
    public String render(DocumentModelBridge document, DocumentModelBridge sheet, Syntax outputSyntax)
    {
        XWikiContext context = getXWikiContext();
        XWiki xwiki = context.getWiki();
        XWikiRightService rightsService = xwiki.getRightService();
        try {
            XWikiDocument targetDocument = getDocument(document);
            XWikiDocument sheetDocument = getDocument(sheet);
            boolean sheetDocumentHasPR = rightsService.hasProgrammingRights(sheetDocument, context);
            boolean targetDocumentHasPR = rightsService.hasProgrammingRights(targetDocument, context);
            if (sheetDocumentHasPR ^ targetDocumentHasPR) {
                // FIXME: If the target document and the sheet don't have the same programming level then we preserve
                // the programming level of the sheet by rendering it as if the author of the target document is the
                // author of the sheet.
                return renderAsSheetAuthor(targetDocument, sheetDocument, outputSyntax);
            } else {
                return render(targetDocument, sheetDocument, outputSyntax);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Renders the sheet in the context of the target document after changing the author of the target document to match
     * the sheet author. This way the level of programming rights available in the sheet is preserved.
     * 
     * @param targetDocument the target document
     * @param sheetDocument the sheet document
     * @param outputSyntax the output syntax
     * @return the result of rendering the sheet in the context of the target document
     */
    private String renderAsSheetAuthor(XWikiDocument targetDocument, XWikiDocument sheetDocument, Syntax outputSyntax)
    {
        DocumentReference targetDocContentAuthorRef = targetDocument.getContentAuthorReference();
        try {
            // This is a hack. We need a better way to preserve the programming rights level of the sheet.
            targetDocument.setContentAuthorReference(sheetDocument.getContentAuthorReference());
            return render(targetDocument, sheetDocument, outputSyntax);
        } finally {
            // Restore the content author of the target document.
            targetDocument.setContentAuthorReference(targetDocContentAuthorRef);
        }
    }

    /**
     * @param document a {@link DocumentModelBridge} instance
     * @return the XWiki document object wrapped by the given {@link DocumentModelBridge} instance
     * @deprecated avoid using this method as much as possible; use the bridge methods instead
     */
    private XWikiDocument getDocument(DocumentModelBridge document)
    {
        return (XWikiDocument) document;
    }

    /**
     * Renders the sheet document in the context of the target document.
     * 
     * @param targetDocument the target document
     * @param sheetDocument the sheet document
     * @param outputSyntax the output syntax
     * @return the result of rendering the sheet document in the context of the target document
     */
    protected abstract String render(XWikiDocument targetDocument, XWikiDocument sheetDocument, Syntax outputSyntax);
}
