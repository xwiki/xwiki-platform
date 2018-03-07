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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.sheet.SheetBinder;
import org.xwiki.sheet.SheetManager;

/**
 * Default {@link SheetManager} implementation.
 * 
 * @version $Id$
 * @since 3.2M3
 */
@Component
@Singleton
public class DefaultSheetManager implements SheetManager
{
    /**
     * The name of the class that describes a sheet.
     */
    private static final String SHEET_CLASS = "XWiki.SheetDescriptorClass";

    /**
     * The object used to log message.
     */
    @Inject
    private Logger logger;

    /**
     * Execution context handler.
     */
    @Inject
    private Execution execution;

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
     * The bridge to the old XWiki core API.
     */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /**
     * The component used to access the XWiki model.
     */
    @Inject
    private ModelBridge modelBridge;

    /**
     * The component used to retrieve the list of sheets bound to a XWiki document.
     */
    @Inject
    @Named("document")
    private SheetBinder documentSheetBinder;

    /**
     * The component used to retrieve the list of sheets bound to a XWiki class.
     */
    @Inject
    @Named("class")
    private SheetBinder classSheetBinder;

    @Override
    public List<DocumentReference> getSheets(DocumentModelBridge document, String action)
    {
        DocumentReference documentReference = document.getDocumentReference();

        // (1) Check if there is a sheet specified for the current execution context. Apply it only if the given
        // document is the current document on the execution context.
        String sheetStringRef = (String) execution.getContext().getProperty("sheet");
        if (sheetStringRef != null && documentReference.equals(documentAccessBridge.getCurrentDocumentReference())) {
            DocumentReference sheetReference = documentReferenceResolver.resolve(sheetStringRef, documentReference);
            if (matchSheet(sheetReference, action)) {
                return Collections.singletonList(sheetReference);
            }
        }

        // (2) Look for document sheets.
        List<DocumentReference> sheets = getDocumentSheets(document, action);
        if (sheets.isEmpty()) {

            // (3) Look for class sheets.
            sheets = new ArrayList<DocumentReference>();
            for (DocumentReference classReference : modelBridge.getXObjectClassReferences(document)) {
                sheets.addAll(getClassSheets(classReference, action));
            }
        }

        return sheets;
    }

    /**
     * @param classReference a reference to a XWiki class
     * @param action the action for which to retrieve the class sheets
     * @return the list of sheets bound to the specified class and matching the specified action; these are sheets
     *         designed to be displayed when the specified action is performed on a document holding an object of the
     *         specified class
     */
    private List<DocumentReference> getClassSheets(DocumentReference classReference, String action)
    {
        DocumentModelBridge classDocument;
        try {
            classDocument = documentAccessBridge.getTranslatedDocumentInstance(classReference);
        } catch (Exception e) {
            String classStringReference = defaultEntityReferenceSerializer.serialize(classReference);
            logger.warn("Failed to get class sheets for [{}]. Reason: [{}]", classStringReference, e.getMessage());
            return Collections.emptyList();
        }
        List<DocumentReference> sheetReferences = new ArrayList<DocumentReference>();
        for (DocumentReference sheetReference : classSheetBinder.getSheets(classDocument)) {
            if (matchSheet(sheetReference, action)) {
                sheetReferences.add(sheetReference);
            }
        }
        return sheetReferences;
    }

    /**
     * @param document the document where to look for sheet references
     * @param action the action for which to retrieve the sheets
     * @return the list of sheets that are referenced by the given document and which are associated with the specified
     *         action
     */
    private List<DocumentReference> getDocumentSheets(DocumentModelBridge document, String action)
    {
        List<DocumentReference> sheets = new ArrayList<DocumentReference>();
        for (DocumentReference sheetReference : documentSheetBinder.getSheets(document)) {
            if (matchSheet(sheetReference, action)) {
                sheets.add(sheetReference);
            }
        }
        return sheets;
    }

    /**
     * @param sheetReference specifies the sheet that is matched
     * @param expectedAction the expected value of the action sheet property
     * @return {@code true} if the given document reference points to a sheet that matches the action and display
     *         constraints
     */
    private boolean matchSheet(DocumentReference sheetReference, String expectedAction)
    {
        if (!documentAccessBridge.exists(sheetReference)) {
            return false;
        }

        DocumentReference sheetClassReference = documentReferenceResolver.resolve(SHEET_CLASS, sheetReference);
        String actualAction = (String) documentAccessBridge.getProperty(sheetReference, sheetClassReference, "action");
        // We assume the sheet matches all actions if it doesn't specify an action (is empty).
        // We assume all actions are matched if the expected value is not specified (is empty).
        return StringUtils.isEmpty(expectedAction) || StringUtils.isEmpty(actualAction)
            || actualAction.equals(expectedAction);
    }
}
