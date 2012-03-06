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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.display.internal.DocumentDisplayer;
import org.xwiki.display.internal.DocumentDisplayerParameters;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.sheet.SheetManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.user.api.XWikiRightService;

/**
 * Displays documents by applying their corresponding sheets. Sheets are rendered in the context of the displayed
 * document. This displayer ensures the programming rights of the sheet are preserved: if the sheet doesn't have
 * programming rights then it is evaluated without them, otherwise, if the sheet has programming rights, it is evaluated
 * with programming rights even if the displayed document doesn't have them.
 * 
 * @version $Id$
 * @since 3.2M3
 */
@Component
@Named("sheet")
@Singleton
public class SheetDocumentDisplayer implements DocumentDisplayer
{
    /** Logging helper object. */
    @Inject
    private Logger logger;

    /**
     * The document displayer used to display the sheets.
     */
    @Inject
    private DocumentDisplayer documentDisplayer;

    /**
     * The component used to retrieve the sheet corresponding to a given document.
     */
    @Inject
    private SheetManager sheetManager;

    /**
     * Execution context handler, needed for accessing the XWikiContext.
     */
    @Inject
    private Execution execution;

    /**
     * The document access bridge.
     */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /**
     * The component used to serialize entity references.
     */
    @Inject
    private EntityReferenceSerializer<String> defaultEntityReferenceSerializer;

    @Override
    public XDOM display(DocumentModelBridge document, DocumentDisplayerParameters parameters)
    {
        if (isSheetExpected(document, parameters)) {
            for (DocumentReference sheetReference : getSheetReferences(document)) {
                if (document.getDocumentReference().equals(sheetReference)) {
                    // If the sheet is the document itself then we simply display the document. We handle this case
                    // differently because unsaved document changes might be ignored if we render the sheet (which is
                    // loaded from the database). So in this case applying the sheet would actually mean displaying the
                    // saved version of the document.
                    break;
                } else if (documentAccessBridge.isDocumentViewable(sheetReference)) {
                    try {
                        return applySheet(document, sheetReference, parameters);
                    } catch (Exception e) {
                        String sheetStringReference = defaultEntityReferenceSerializer.serialize(sheetReference);
                        logger.warn("Failed to apply sheet [{}].", sheetStringReference, e);
                    }
                }
            }
        }

        return documentDisplayer.display(document, parameters);
    }

    /**
     * @param document the document that is displayed
     * @param parameters the display parameters
     * @return {@code true} if the context in which this displayer is called is expecting the document sheet to be
     *         displayed, {@code false} otherwise
     */
    private boolean isSheetExpected(DocumentModelBridge document, DocumentDisplayerParameters parameters)
    {
        try {
            // We test if the default edit mode is "edit" to ensure backward compatibility with older XWiki applications
            // that don't use the new sheet system (they most probably use "inline" as the default edit mode).
            return parameters.isContentTransformed()
                && (parameters.isExecutionContextIsolated() || document.getDocumentReference().equals(
                    documentAccessBridge.getCurrentDocumentReference()))
                && "edit".equals(((XWikiDocument) document).getDefaultEditMode(getXWikiContext()));
        } catch (XWikiException e) {
            logger.warn("Failed to get the default edit mode for [{}].",
                defaultEntityReferenceSerializer.serialize(document.getDocumentReference()));
            return false;
        }
    }

    /**
     * @param document a document
     * @return the list of sheet references that can be applied to the given document in the current context
     */
    private List<DocumentReference> getSheetReferences(DocumentModelBridge document)
    {
        XWikiContext xcontext = getXWikiContext();
        // XObjects are shared by all document translations and are accessible only from the default translation. We
        // have to pass the default document translation to the sheet manager because otherwise it won't detect the
        // sheets.
        DocumentModelBridge defaultTranslation = document;
        // Check if the given document is a translation (i.e. if it's not the default translation).
        if (((XWikiDocument) document).getTranslation() != 0) {
            try {
                // Load the default document translation.
                defaultTranslation = xcontext.getWiki().getDocument(document.getDocumentReference(), xcontext);
            } catch (XWikiException e) {
                String stringReference = defaultEntityReferenceSerializer.serialize(document.getDocumentReference());
                logger.warn("Failed to load the default translation of [{}].", stringReference, e);
            }
        }
        return sheetManager.getSheets(defaultTranslation, xcontext.getAction());
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
     * Applies a sheet to a document.
     * 
     * @param document the document to apply the sheet to
     * @param sheetReference the sheet to apply
     * @param parameters the display parameters
     * @return the result of rendering the sheet in the context of the given document
     * @throws Exception if applying the sheet fails
     */
    private XDOM applySheet(DocumentModelBridge document, DocumentReference sheetReference,
        DocumentDisplayerParameters parameters) throws Exception
    {
        DocumentModelBridge sheet = documentAccessBridge.getDocument(sheetReference);
        if (parameters.isTitleDisplayed() && StringUtils.isEmpty(sheet.getTitle())) {
            return documentDisplayer.display(document, parameters);
        }

        if (programmingRightsConflict(document, sheet)) {
            // FIXME: If the displayed document and the sheet don't have the same programming rights then we preserve
            // the programming rights of the sheet by rendering it as if the author of the displayed document is the
            // author of the sheet.
            return displayAsSheetAuthor(document, sheet, parameters);
        } else {
            return display(document, sheet, parameters);
        }
    }

    /**
     * @param document a document
     * @param sheet a sheet
     * @return {@code true} if the sheet and the document have different programming rights.
     */
    private boolean programmingRightsConflict(DocumentModelBridge document, DocumentModelBridge sheet)
    {
        XWikiContext context = getXWikiContext();
        XWikiRightService rightsService = context.getWiki().getRightService();
        return rightsService.hasProgrammingRights((XWikiDocument) document, context)
            ^ rightsService.hasProgrammingRights((XWikiDocument) sheet, context);
    }

    /**
     * Displays a document with a sheet, changing the document content author to match the sheet content author in order
     * to preserve the programming rights of the sheet.
     * 
     * @param document the displayed document
     * @param sheet the applied sheet
     * @param parameters the display parameters
     * @return the result of displaying the sheet in the context of the given document
     * @throws Exception if displaying the sheet fails
     */
    private XDOM displayAsSheetAuthor(DocumentModelBridge document, DocumentModelBridge sheet,
        DocumentDisplayerParameters parameters) throws Exception
    {
        DocumentReference docContentAuthorRef = ((XWikiDocument) document).getContentAuthorReference();
        try {
            // This is a hack. We need a better way to preserve the programming rights level of the sheet.
            ((XWikiDocument) document).setContentAuthorReference(((XWikiDocument) sheet).getContentAuthorReference());
            return display(document, sheet, parameters);
        } finally {
            // Restore the content author of the target document.
            ((XWikiDocument) document).setContentAuthorReference(docContentAuthorRef);
        }
    }

    /**
     * Displays a document with a sheet.
     * 
     * @param document the displayed document
     * @param sheet the applied sheet
     * @param parameters the display parameters
     * @return the result of displaying the sheet in the context of the given document
     * @throws Exception if displaying the sheet fails
     */
    private XDOM display(DocumentModelBridge document, DocumentModelBridge sheet,
        DocumentDisplayerParameters parameters) throws Exception
    {
        Map<String, Object> backupObjects = null;
        try {
            if (!document.getDocumentReference().equals(documentAccessBridge.getCurrentDocumentReference())) {
                backupObjects = new HashMap<String, Object>();
                // The following method call also clones the execution context.
                documentAccessBridge.pushDocumentInContext(backupObjects, document.getDocumentReference());
            }
            DocumentDisplayerParameters sheetDisplayParameters = parameters.clone();
            sheetDisplayParameters.setExecutionContextIsolated(false);
            return documentDisplayer.display(sheet, sheetDisplayParameters);
        } finally {
            if (backupObjects != null) {
                documentAccessBridge.popDocumentFromContext(backupObjects);
            }
        }
    }
}
