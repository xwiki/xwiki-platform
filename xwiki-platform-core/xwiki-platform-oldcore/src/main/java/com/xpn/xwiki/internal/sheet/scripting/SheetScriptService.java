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
package com.xpn.xwiki.internal.sheet.scripting;

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
import org.xwiki.script.service.ScriptService;
import org.xwiki.sheet.SheetManager;

import com.xpn.xwiki.api.Document;

/**
 * Exposes {@link SheetManager} to Velocity scripts.
 * 
 * @version $Id$
 * @since 3.2M3
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
     * The component used to check access rights on sheets.
     */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /**
     * @param document the document for which to retrieve the sheets
     * @param action the action for which to retrieve the sheets ('view', 'edit' etc.)
     * @return the list of sheets available for the specified document on the specified action; multiple sheets can be
     *         aggregated and displayed in the content area of the generated HTML page
     */
    public List<DocumentReference> getSheets(Document document, String action)
    {
        if (documentAccessBridge.isDocumentViewable(document.getDocumentReference())) {
            return filterViewable(sheetManager.getSheets(getDocument(document), action));
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
