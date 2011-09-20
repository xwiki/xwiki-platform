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
package com.xpn.xwiki.internal.sheet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.sheet.SheetBinder;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Abstract {@link SheetBinder} implementation that binds a sheet to a XWiki document by adding an object to the
 * document. The object has a property named "sheet" that holds a reference to the sheet. Concrete extension of this
 * class must specify the type of object to be used for binding.
 * 
 * @version $Id$
 * @since 3.2M3
 */
public abstract class AbstractSheetBinder implements SheetBinder
{
    /**
     * The name of the property of the binding object that holds the reference to the sheet.
     */
    private static final String SHEET_PROPERTY = "sheet";

    /** Logging helper object. */
    @Inject
    private Logger logger;

    /**
     * The component used to resolve a string document reference.
     */
    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    /**
     * The component used to serialize entity references.
     */
    @Inject
    private EntityReferenceSerializer<String> defaultEntityReferenceSerializer;

    /**
     * Execution context handler, needed for accessing the XWikiContext.
     */
    @Inject
    private Execution execution;

    @Override
    public List<DocumentReference> getSheets(DocumentModelBridge document)
    {
        DocumentReference sheetBindingClassReference =
            documentReferenceResolver.resolve(getSheetBindingClass(), document.getDocumentReference());
        List<BaseObject> sheetBindingObjects = ((XWikiDocument) document).getXObjects(sheetBindingClassReference);
        if (sheetBindingObjects == null) {
            return Collections.emptyList();
        }
        List<DocumentReference> sheets = new ArrayList<DocumentReference>();
        for (BaseObject sheetBindingObject : sheetBindingObjects) {
            String sheetStringRef = sheetBindingObject.getStringValue(SHEET_PROPERTY);
            DocumentReference sheetReference =
                documentReferenceResolver.resolve(sheetStringRef, document.getDocumentReference());
            sheets.add(sheetReference);
        }
        return sheets;
    }

    @Override
    public boolean bind(DocumentModelBridge document, DocumentReference sheetReference)
    {
        DocumentReference sheetBindingClassReference =
            documentReferenceResolver.resolve(getSheetBindingClass(), document.getDocumentReference());
        List<BaseObject> sheetBindingObjects = ((XWikiDocument) document).getXObjects(sheetBindingClassReference);
        if (sheetBindingObjects != null) {
            for (BaseObject sheetBindingObject : sheetBindingObjects) {
                String boundSheetStringRef = sheetBindingObject.getStringValue(SHEET_PROPERTY);
                DocumentReference boundSheetReference =
                    documentReferenceResolver.resolve(boundSheetStringRef, document.getDocumentReference());
                if (boundSheetReference.equals(sheetReference)) {
                    return false;
                }
            }
        }
        String sheetStringReference =
            defaultEntityReferenceSerializer.serialize(sheetReference, document.getDocumentReference());
        try {
            BaseObject sheetBindingObject =
                ((XWikiDocument) document).newXObject(sheetBindingClassReference, getXWikiContext());
            sheetBindingObject.setStringValue(SHEET_PROPERTY, sheetStringReference);
        } catch (XWikiException e) {
            logger.warn("Failed to bind sheet [{}] to document [{}].", sheetStringReference,
                defaultEntityReferenceSerializer.serialize(document.getDocumentReference()));
            return false;
        }
        return true;
    }

    @Override
    public boolean unbind(DocumentModelBridge document, DocumentReference sheetReference)
    {
        DocumentReference sheetBindingClassReference =
            documentReferenceResolver.resolve(getSheetBindingClass(), document.getDocumentReference());
        List<BaseObject> sheetBindingObjects = ((XWikiDocument) document).getXObjects(sheetBindingClassReference);
        if (sheetBindingObjects == null) {
            return false;
        }
        for (BaseObject sheetBindingObject : sheetBindingObjects) {
            String boundSheetStringRef = sheetBindingObject.getStringValue(SHEET_PROPERTY);
            DocumentReference boundSheetReference =
                documentReferenceResolver.resolve(boundSheetStringRef, document.getDocumentReference());
            if (boundSheetReference.equals(sheetReference)) {
                return ((XWikiDocument) document).removeXObject(sheetBindingObject);
            }
        }
        return false;
    }

    /**
     * @return the XWiki context
     * @deprecated avoid using this method; try using the document access bridge instead
     */
    private XWikiContext getXWikiContext()
    {
        return (XWikiContext) execution.getContext().getProperty("xwikicontext");
    }

    /**
     * @return the string reference of the class used to bind sheets to documents
     */
    protected abstract String getSheetBindingClass();
}
