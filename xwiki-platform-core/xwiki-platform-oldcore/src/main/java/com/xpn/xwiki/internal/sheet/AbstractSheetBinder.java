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
import javax.inject.Named;

import org.slf4j.Logger;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.context.Execution;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
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
public abstract class AbstractSheetBinder implements SheetBinder, Initializable
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
     * The component used to resolve a string relative reference.
     */
    @Inject
    @Named("relative")
    private EntityReferenceResolver<String> relativeReferenceResolver;
    
    /**
     * The component used to serialize entity references as absolute string references.
     */
    @Inject
    private EntityReferenceSerializer<String> defaultEntityReferenceSerializer;

    /**
     * The component used to serialize entity references as relative string references.
     */
    @Inject
    @Named("compact")
    private EntityReferenceSerializer<String> compactEntityReferenceSerializer;

    /**
     * Execution context handler, needed for accessing the XWikiContext.
     */
    @Inject
    private Execution execution;

    /**
     * The component used to create database queries.
     */
    @Inject
    private QueryManager queryManager;

    /**
     * The query used to retrieve the list of all sheet bindings.
     */
    private Query sheetBindingsQuery;

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
            // The list of XWiki objects can contain null values due to a design flaw in the old XWiki core.
            if (sheetBindingObject != null) {
                String sheetStringRef = sheetBindingObject.getStringValue(SHEET_PROPERTY);
                DocumentReference sheetReference =
                    documentReferenceResolver.resolve(sheetStringRef, document.getDocumentReference());
                sheets.add(sheetReference);
            }
        }
        return sheets;
    }

    @Override
    public List<DocumentReference> getDocuments(DocumentReference expectedSheetRef)
    {
        sheetBindingsQuery.setWiki(expectedSheetRef.getWikiReference().getName());
        try {
            List<String[]> sheetBindings = sheetBindingsQuery.execute();
            List<DocumentReference> documentReferences = new ArrayList<DocumentReference>();
            for (String[] sheetBinding : sheetBindings) {
                DocumentReference docRef = documentReferenceResolver.resolve(sheetBinding[0], expectedSheetRef);
                DocumentReference sheetRef = documentReferenceResolver.resolve(sheetBinding[1], docRef);
                if (sheetRef.equals(expectedSheetRef)) {
                    documentReferences.add(docRef);
                }
            }
            return documentReferences;
        } catch (QueryException e) {
            logger.warn("Failed to query sheet bindings.", e);
            return Collections.emptyList();
        }
    }

    @Override
    public boolean bind(DocumentModelBridge document, DocumentReference sheetReference)
    {
        EntityReference sheetBindingClassReference =
            this.relativeReferenceResolver.resolve(getSheetBindingClass(), EntityType.DOCUMENT);
        List<BaseObject> sheetBindingObjects = ((XWikiDocument) document).getXObjects(sheetBindingClassReference);
        if (sheetBindingObjects != null) {
            for (BaseObject sheetBindingObject : sheetBindingObjects) {
                // The list of XWiki objects can contain null values due to a design flaw in the old XWiki core.
                if (sheetBindingObject != null) {
                    String boundSheetStringRef = sheetBindingObject.getStringValue(SHEET_PROPERTY);
                    DocumentReference boundSheetReference =
                        documentReferenceResolver.resolve(boundSheetStringRef, document.getDocumentReference());
                    if (boundSheetReference.equals(sheetReference)) {
                        return false;
                    }
                }
            }
        }
        String relativeSheetStringReference =
            compactEntityReferenceSerializer.serialize(sheetReference, document.getDocumentReference());
        try {
            BaseObject sheetBindingObject =
                ((XWikiDocument) document).newXObject(sheetBindingClassReference, getXWikiContext());
            sheetBindingObject.setStringValue(SHEET_PROPERTY, relativeSheetStringReference);
        } catch (XWikiException e) {
            String docStringReference = defaultEntityReferenceSerializer.serialize(document.getDocumentReference());
            String sheetStringReference = defaultEntityReferenceSerializer.serialize(sheetReference);
            logger.warn("Failed to bind sheet [{}] to document [{}].", sheetStringReference, docStringReference);
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
            // The list of XWiki objects can contain null values due to a design flaw in the old XWiki core.
            if (sheetBindingObject != null) {
                String boundSheetStringRef = sheetBindingObject.getStringValue(SHEET_PROPERTY);
                DocumentReference boundSheetReference =
                    documentReferenceResolver.resolve(boundSheetStringRef, document.getDocumentReference());
                if (boundSheetReference.equals(sheetReference)) {
                    return ((XWikiDocument) document).removeXObject(sheetBindingObject);
                }
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

    @Override
    public void initialize() throws InitializationException
    {
        try {
            String statement =
                "select doc.fullName, prop.value from XWikiDocument doc, BaseObject obj, StringProperty prop where "
                    + "obj.className=:sheetBindingClass and obj.name=doc.fullName and obj.id=prop.id.id and "
                    + "prop.id.name=:sheetProperty order by doc.fullName";
            sheetBindingsQuery = queryManager.createQuery(statement, Query.HQL);
            sheetBindingsQuery.bindValue("sheetBindingClass", getSheetBindingClass());
            sheetBindingsQuery.bindValue("sheetProperty", SHEET_PROPERTY);
        } catch (QueryException e) {
            throw new InitializationException("Failed to create query for retrieving the list of sheet bindings.", e);
        }
    }
}
