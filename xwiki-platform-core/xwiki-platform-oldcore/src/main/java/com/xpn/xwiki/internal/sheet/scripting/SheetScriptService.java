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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.sheet.SheetBinder;
import org.xwiki.sheet.SheetManager;

import com.xpn.xwiki.api.Document;

/**
 * Exposes {@link SheetManager} and {@link SheetBinder} to Velocity scripts.
 * 
 * @version $Id$
 * @since 3.2M3
 */
@Component
@Named("sheet")
@Singleton
public class SheetScriptService implements ScriptService
{
    /**
     * The component used to manage the sheets.
     */
    @Inject
    private SheetManager sheetManager;

    /**
     * The component used to manage the class sheet bindings.
     */
    @Inject
    @Named("class")
    private SheetBinder classSheetBinder;

    /**
     * The component used to manage the document sheet bindings.
     */
    @Inject
    @Named("document")
    private SheetBinder documentSheetBinder;

    /**
     * The component used to check access rights on sheets.
     */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /**
     * Returns the list of sheets associated with a XWiki document.
     * 
     * @param document the document for which to retrieve the sheets
     * @param action the action for which to retrieve the sheets ('view', 'edit' etc.)
     * @return the list of sheets available for the specified document on the specified action; these are sheets
     *         designed to be displayed when the specified action is performed on the given document; only the sheets
     *         that the current user has the right to view are returned
     */
    public List<DocumentReference> getSheets(Document document, String action)
    {
        return filterViewable(sheetManager.getSheets(getReadOnlyDocument(document), action));
    }

    /**
     * Retrieves the list of sheets explicitly bound to a XWiki class.
     * 
     * @param classDocument a document holding a class definition
     * @return the list of sheets explicitly bound to the given class; these are sheets designed to be applied to
     *         documents that have objects of the given class; only the sheets that the current user has the right to
     *         view are returned
     */
    public List<DocumentReference> getClassSheets(Document classDocument)
    {
        return filterViewable(classSheetBinder.getSheets(getReadOnlyDocument(classDocument)));
    }

    /**
     * Binds a sheet to a XWiki class. Changes are not persisted until the class document is saved.
     * 
     * @param classDocument a document holding a class definition
     * @param sheetReference a reference to a sheet
     * @return {@code true} if the sheet was successfully bound, {@code false} otherwise
     */
    public boolean bindClassSheet(Document classDocument, DocumentReference sheetReference)
    {
        return classSheetBinder.bind(getModifiableDocument(classDocument), sheetReference);
    }

    /**
     * Removes the binding between a XWiki class and a sheet. Changes are not persisted until the class document is
     * saved.
     * 
     * @param classDocument a document holding a class definition
     * @param sheetReference a reference to a sheet
     * @return {@code true} if the sheet was successfully unbound, {@code false} otherwise
     */
    public boolean unbindClassSheet(Document classDocument, DocumentReference sheetReference)
    {
        return classSheetBinder.unbind(getModifiableDocument(classDocument), sheetReference);
    }

    /**
     * Retrieves the list of sheets explicitly bound to a XWiki document.
     * 
     * @param document a XWiki document
     * @return the list of sheets explicitly bound to the given document; only the sheets that the current user has the
     *         right to view are returned
     */
    public List<DocumentReference> getDocumentSheets(Document document)
    {
        return filterViewable(documentSheetBinder.getSheets(getReadOnlyDocument(document)));
    }

    /**
     * Binds a sheet to a XWiki document. Changes are not persisted until the document is saved.
     * 
     * @param document a XWiki document
     * @param sheetReference a reference to a sheet
     * @return {@code true} if the sheet was successfully bound, {@code false} otherwise
     */
    public boolean bindDocumentSheet(Document document, DocumentReference sheetReference)
    {
        return documentSheetBinder.bind(getModifiableDocument(document), sheetReference);
    }

    /**
     * Removes the binding between a XWiki document and a sheet. Changes are not persisted until the document is saved.
     * 
     * @param document a XWiki document
     * @param sheetReference a reference to a sheet
     * @return {@code true} if the sheet was successfully unbound, {@code false} otherwise
     */
    public boolean unbindDocumentSheet(Document document, DocumentReference sheetReference)
    {
        return documentSheetBinder.unbind(getModifiableDocument(document), sheetReference);
    }

    /**
     * Retrieves the list of documents that have explicitly bound the specified sheet.
     * 
     * @param sheetReference a reference to a sheet
     * @return the list of documents that have the specified sheet explicitly bound
     */
    public List<DocumentReference> getDocuments(DocumentReference sheetReference)
    {
        List<DocumentReference> documents = new ArrayList<DocumentReference>();
        documents.addAll(documentSheetBinder.getDocuments(sheetReference));
        documents.addAll(classSheetBinder.getDocuments(sheetReference));
        return filterViewable(documents);
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
     *         document API and that <em>must</em> not be modified
     */
    private DocumentModelBridge getReadOnlyDocument(Document document)
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

    /**
     * Note: This method gets the low level XWiki document through reflection by calling a protected method.
     * 
     * @param document an instance of {@link Document} received from a script
     * @return an instance of {@link DocumentModelBridge} that wraps the low level document object exposed by the given
     *         document API and that can be <em>safely</em> modified
     */
    private DocumentModelBridge getModifiableDocument(Document document)
    {
        try {
            // HACK: We try to get the modifiable XWikiDocument instance wrapped by the document API using reflection
            // because the corresponding method that clones the wrapped XWikiDocument instance is protected.
            Method getDocMethod = Document.class.getDeclaredMethod("getDoc", new Class[] {});
            getDocMethod.setAccessible(true);
            return (DocumentModelBridge) getDocMethod.invoke(document);
        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to get the modifiable XWikiDocument instance wrapped by the document API.", e);
        }
    }
}
