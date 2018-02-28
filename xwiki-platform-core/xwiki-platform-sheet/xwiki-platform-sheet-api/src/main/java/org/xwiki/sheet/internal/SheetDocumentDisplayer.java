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
import org.xwiki.display.internal.DocumentDisplayer;
import org.xwiki.display.internal.DocumentDisplayerParameters;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.sheet.SheetManager;

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
     * The document access bridge.
     */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /**
     * The component used to access the XWiki model.
     */
    @Inject
    private ModelBridge modelBridge;

    @Inject
    private ModelContext modelContext;

    /**
     * The component used to serialize entity references.
     */
    @Inject
    private EntityReferenceSerializer<String> defaultEntityReferenceSerializer;

    @Override
    public XDOM display(DocumentModelBridge document, DocumentDisplayerParameters parameters)
    {
        XDOM xdom = null;
        if (isSheetExpected(document, parameters)) {
            Map<String, Object> backupObjects = null;
            EntityReference currentWikiReference = null;
            try {
                // It is very important to determine the sheet in a new, isolated, execution context, if the given
                // document is not currently on the execution context. Put the given document in the context only if
                // it's not already there.
                if (!modelBridge.isCurrentDocument(document)) {
                    backupObjects = modelBridge.pushDocumentInContext(document);

                    currentWikiReference = modelContext.getCurrentEntityReference();
                    modelContext.setCurrentEntityReference(document.getDocumentReference().getWikiReference());
                }
                xdom = maybeDisplayWithSheet(document, parameters);
            } finally {
                if (backupObjects != null) {
                    documentAccessBridge.popDocumentFromContext(backupObjects);
                    modelContext.setCurrentEntityReference(currentWikiReference);
                }
            }
        }

        // Fall back on the default document displayer if no sheet was applied. Note that we don't isolate the context
        // before calling the default document displayer. It is better to let the default document displayer choose if
        // isolating the execution context is needed due to the way #isSheetExpected() checks for current document.
        return xdom != null ? xdom : documentDisplayer.display(document, parameters);
    }

    /**
     * @param document the document that is displayed
     * @param parameters the display parameters
     * @return {@code true} if the context in which this displayer is called is expecting the document sheet to be
     *         displayed, {@code false} otherwise
     */
    private boolean isSheetExpected(DocumentModelBridge document, DocumentDisplayerParameters parameters)
    {
        // We make 3 checks:
        //
        // (1) If the content is not transformed then the goal is probably to include it in a bigger XDOM to be rendered
        // later, in which case the actual document content is needed, not the content of the sheet.
        //
        // (2) If the execution context is not isolated then the document is probably included in another one
        // (context=current) so we need to display the actual document content. Note that we also test if the given
        // document is the current one because requested documents (e.g. /view/Space/Page) are displayed from Velocity
        // templates using $doc.getRenderedContent() which doesn't isolate the execution context. We don't test if the
        // document instances are the same but if their references are equal because Velocity templates use various
        // document variables ($doc, $tdoc, $cdoc, etc.) to call getRenderedContent().
        //
        // (3) We test if the default edit mode is "edit" to ensure backward compatibility with older XWiki applications
        // that don't use the new sheet system (they most probably use "inline" as the default edit mode).
        return parameters.isContentTransformed()
            && (parameters.isExecutionContextIsolated() || document.getDocumentReference().equals(
                documentAccessBridge.getCurrentDocumentReference()))
            && "edit".equals(modelBridge.getDefaultEditMode(document));
    }

    /**
     * Iterates the list of sheets bound to the given document and tries to applies them, stopping after the first
     * success. If none of the sheets can be applied (e.g. insufficient rights) then it displays the document without a
     * sheet, using the default document displayer.
     * 
     * @param document the document that is displayed
     * @param parameters the display parameters
     * @return the result of displaying the given document, with or without a sheet
     */
    private XDOM maybeDisplayWithSheet(DocumentModelBridge document, DocumentDisplayerParameters parameters)
    {
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

        // No sheet was applied. Fall back on the default document displayer.
        return null;
    }

    /**
     * @param document a document
     * @return the list of sheet references that can be applied to the given document in the current context
     */
    private List<DocumentReference> getSheetReferences(DocumentModelBridge document)
    {
        // XObjects are shared by all document translations and are accessible only from the default translation. We
        // have to pass the default document translation to the sheet manager because otherwise it won't detect the
        // sheets.
        return sheetManager.getSheets(modelBridge.getDefaultTranslation(document), modelBridge.getCurrentAction());
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
        DocumentModelBridge sheet = documentAccessBridge.getTranslatedDocumentInstance(sheetReference);
        if (parameters.isTitleDisplayed() && StringUtils.isEmpty(sheet.getTitle())) {
            // The sheet doesn't control the title. Fall back on the default document displayer.
            return null;
        }

        DocumentModelBridge originalSecurityDoc = this.modelBridge.setSecurityDocument(sheet);

        try {
            return display(document, sheet, parameters);
        } finally {
            this.modelBridge.setSecurityDocument(originalSecurityDoc);
        }
    }

    /**
     * Displays a document with a sheet.
     * 
     * @param document the displayed document
     * @param sheet the applied sheet
     * @param parameters the display parameters
     * @return the result of displaying the sheet in the context of the given document
     */
    private XDOM display(DocumentModelBridge document, DocumentModelBridge sheet,
        DocumentDisplayerParameters parameters)
    {
        DocumentDisplayerParameters sheetDisplayParameters = parameters.clone();
        // The execution context was already isolated and we want to display the sheet in the context of the given doc.
        sheetDisplayParameters.setExecutionContextIsolated(false);
        return documentDisplayer.display(sheet, sheetDisplayParameters);
    }
}
