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
package org.xwiki.sheet.internal.scripting;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.service.ScriptService;
import org.xwiki.sheet.SheetManager;
import org.xwiki.sheet.SheetManager.SheetDisplay;
import org.xwiki.sheet.SheetRenderer;

import com.xpn.xwiki.api.Document;

/**
 * Exposes {@link SheetManager} and {@link SheetRenderer} to Velocity scripts.
 * 
 * @version $Id$
 */
@Component
@Named("sheet")
public class SheetScriptService implements ScriptService
{
    /**
     * The component used to manage the sheets.
     */
    @Inject
    private SheetManager sheetManager;

    /**
     * The component used to render the content of the sheet.
     */
    @Inject
    @Named("content")
    private SheetRenderer contentSheetRenderer;

    /**
     * The component used to render the title of the sheet.
     */
    @Inject
    @Named("title")
    private SheetRenderer titleSheetRenderer;

    /**
     * The component used to check access rights on sheets.
     */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /**
     * @param document the document that is being rendered
     * @param action the action taken on the rendered document ('view', 'edit' etc.)
     * @return the list of {@code page} sheets available for the specified document on the specified action; a page
     *         sheet controls most of the generated HTML page; only one page sheet can be applied to a document
     */
    public DocumentReference getPageSheet(Document document, String action)
    {
        if (documentAccessBridge.isDocumentViewable(document.getDocumentReference())) {
            List<DocumentReference> pageSheets =
                filterViewable(sheetManager.getSheets(getDocument(document), action, SheetDisplay.PAGE));
            return pageSheets.isEmpty() ? null : pageSheets.get(0);
        }
        return null;
    }

    /**
     * @param document the document that is being rendered
     * @param action the action taken on the rendered document ('view', 'edit' etc.)
     * @return the list of {@code inline} sheets available for the specified document on the specified action; multiple
     *         in-line sheets can be aggregated and displayed in the content area of the generated HTML page
     */
    public List<DocumentReference> getInlineSheets(Document document, String action)
    {
        if (documentAccessBridge.isDocumentViewable(document.getDocumentReference())) {
            return filterViewable(sheetManager.getSheets(getDocument(document), action, SheetDisplay.INLINE));
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * @param classReference a reference to a document holding a class definition
     * @param action the action for which to retrieve the sheet
     * @return a sheet that can be used to render object of the specified class, for the specified action
     */
    public DocumentReference getClassSheet(DocumentReference classReference, String action)
    {
        if (documentAccessBridge.isDocumentViewable(classReference)) {
            DocumentReference sheetReference = sheetManager.getClassSheet(classReference, action);
            if (sheetReference != null && documentAccessBridge.isDocumentViewable(sheetReference)) {
                return sheetReference;
            }
        }
        return null;
    }

    /**
     * @param documentReferences a list of document references
     * @return only the references that point to documents that can be viewed by the current user
     */
    private List<DocumentReference> filterViewable(List<DocumentReference> documentReferences)
    {
        List<DocumentReference> viewable = new ArrayList<DocumentReference>();
        for (DocumentReference documentReference : documentReferences) {
            if (documentAccessBridge.isDocumentViewable(documentReference)) {
                viewable.add(documentReference);
            }
        }
        return viewable;
    }

    /**
     * Applies a sheet to a document by rendering the content of the sheet in the context of the document. View rights
     * are required both on the sheet and on the target document. This method ensures the programming rights of the
     * sheet are preserved: if the sheet doesn't have programming rights then its content is evaluated without them,
     * otherwise, if the sheet has programming rights, the content is evaluated with programming rights even if the
     * target document doesn't have them.
     * 
     * @param document the target document, i.e. the document the sheet is applied to
     * @param sheet the sheet whose content is rendered
     * @param outputSyntax the output syntax
     * @return the result of rendering the content of the specified sheet in the context of the target document
     */
    public String renderContent(Document document, Document sheet, Syntax outputSyntax)
    {
        if (documentAccessBridge.isDocumentViewable(document.getDocumentReference())
            && documentAccessBridge.isDocumentViewable(sheet.getDocumentReference())) {
            return contentSheetRenderer.render(getDocument(document), getDocument(sheet), outputSyntax);
        }
        return null;
    }

    /**
     * Applies a sheet to a document by rendering the title of the sheet in the context of the document. View rights are
     * required both on the sheet and on the target document. This method ensures the programming rights of the sheet
     * are preserved: if the sheet doesn't have programming rights then its title is evaluated without them, otherwise,
     * if the sheet has programming rights, the title is evaluated with programming rights even if the target document
     * doesn't have them.
     * 
     * @param document the target document, i.e. the document the sheet is applied to
     * @param sheet the sheet whose title is rendered
     * @param outputSyntax the output syntax
     * @return the result of rendering the title of the specified sheet in the context of the target document
     */
    public String renderTitle(Document document, Document sheet, Syntax outputSyntax)
    {
        if (documentAccessBridge.isDocumentViewable(document.getDocumentReference())
            && documentAccessBridge.isDocumentViewable(sheet.getDocumentReference())) {
            return titleSheetRenderer.render(getDocument(document), getDocument(sheet), outputSyntax);
        }
        return null;
    }

    /**
     * Note: This method accesses the low level XWiki document through reflection in order to bypass programming rights.
     * 
     * @param document an instance of {@link Document} received from a script
     * @return an instance of {@link DocumentModelBridge} that wraps the low level document object exposed by the given
     *         document API
     */
    private DocumentModelBridge getDocument(Document document)
    {
        try {
            // HACK: We try to access the XWikiDocument instance wrapped by the document API using reflection because we
            // want to bypass the programming rights requirements.
            Field docField = Document.class.getDeclaredField("doc");
            docField.setAccessible(true);
            return (DocumentModelBridge) docField.get(document);
        } catch (Exception e) {
            throw new RuntimeException("Failed to access the XWikiDocument instance wrapped by the document API.", e);
        }
    }
}
