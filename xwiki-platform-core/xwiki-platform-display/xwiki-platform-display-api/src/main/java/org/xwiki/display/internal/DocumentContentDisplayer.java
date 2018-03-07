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
package org.xwiki.display.internal;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.HeaderBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.BlockMatcher;
import org.xwiki.rendering.block.match.ClassBlockMatcher;
import org.xwiki.rendering.block.match.CompositeBlockMatcher;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.parser.ContentParser;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.rendering.transformation.TransformationManager;
import org.xwiki.velocity.VelocityManager;

/**
 * Displays the content of a document.
 * 
 * @version $Id$
 * @since 3.2M3
 */
@Component
@Named("content")
@Singleton
public class DocumentContentDisplayer implements DocumentDisplayer
{
    /**
     * The context property which indicates if the current code was called from a template (only Velocity execution) or
     * from a wiki page (wiki syntax rendering).
     */
    private static final String IS_IN_RENDERING_ENGINE = "isInRenderingEngine";

    /**
     * The object used for logging.
     */
    @Inject
    private Logger logger;

    /**
     * The component used to serialize entity references.
     */
    @Inject
    private EntityReferenceSerializer<String> defaultEntityReferenceSerializer;

    /**
     * Used to get the current document reference and to change the context document.
     */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /**
     * Execution context handler, needed for accessing the XWiki context map.
     */
    @Inject
    private Execution execution;

    /**
     * The object used to access the Velocity engine.
     */
    @Inject
    private VelocityManager velocityManager;

    /**
     * The component used to execute the transformations on the displayed content.
     */
    @Inject
    private TransformationManager transformationManager;

    @Inject
    private ModelContext modelContext;

    /**
     * Used to get a parser for a specific syntax.
     */
    @Inject
    private ContentParser parser;

    @Override
    public XDOM display(DocumentModelBridge document, DocumentDisplayerParameters parameters)
    {
        String nameSpace =
            defaultEntityReferenceSerializer.serialize(parameters.isContentTransformed()
                && parameters.isTransformationContextIsolated() ? document.getDocumentReference()
                : documentAccessBridge.getCurrentDocumentReference());

        // This tells display() methods that we are inside the rendering engine and thus that they can return wiki
        // syntax and not HTML syntax (which is needed when outside the rendering engine, i.e. when we're inside
        // templates using only Velocity for example).
        Map<Object, Object> xwikiContext = getXWikiContextMap();
        Object isInRenderingEngine = xwikiContext.put(IS_IN_RENDERING_ENGINE, true);

        maybeOpenNameSpace(nameSpace, parameters.isTransformationContextIsolated(), isInRenderingEngine);

        try {
            XDOM result =
                parameters.isExecutionContextIsolated() ? displayInIsolatedExecutionContext(document, nameSpace,
                    parameters) : display(document, nameSpace, parameters);
            return result;
        } finally {
            // Since we configure Velocity to have local macros (i.e. macros visible only to the local context), since
            // Velocity caches the velocity macros in a local cache (we use key which is the absolute document
            // reference) and since documents can include other documents or panels, we need to make sure we empty the
            // local Velocity macro cache at the end of the rendering for the document as otherwise the local Velocity
            // macro caches will keep growing as users create new pages.
            maybeCloseNameSpace(nameSpace, parameters.isTransformationContextIsolated(), isInRenderingEngine);

            if (isInRenderingEngine != null) {
                xwikiContext.put(IS_IN_RENDERING_ENGINE, isInRenderingEngine);
            } else {
                xwikiContext.remove(IS_IN_RENDERING_ENGINE);
            }
        }
    }

    /**
     * @return the XWiki context map
     */
    @SuppressWarnings("unchecked")
    private Map<Object, Object> getXWikiContextMap()
    {
        return (Map<Object, Object>) execution.getContext().getProperty("xwikicontext");
    }

    /**
     * Opens the specified name-space if the transformation context is isolated and we are not in the rendering engine.
     * 
     * @param nameSpace the name-space to open
     * @param transformationContextIsolated whether the transformation context is isolated
     * @param isInRenderingEngine whether we are in the rendering engine
     */
    private void maybeOpenNameSpace(String nameSpace, boolean transformationContextIsolated, Object isInRenderingEngine)
    {
        if (transformationContextIsolated && (isInRenderingEngine == null || isInRenderingEngine == Boolean.FALSE)) {
            try {
                // Mark that we're starting to use a different Velocity macro name-space.
                velocityManager.getVelocityEngine().startedUsingMacroNamespace(nameSpace);
                logger.debug("Started using velocity macro namespace [{}].", nameSpace);
            } catch (Exception e) {
                // Failed to get the Velocity Engine and thus to clear Velocity Macro cache. Log this as a warning but
                // continue since it's not absolutely critical.
                logger.warn("Failed to notify Velocity Macro cache for opening the [{}] namespace. Reason = [{}]",
                    nameSpace, ExceptionUtils.getRootCauseMessage(e));
            }
        }
    }

    /**
     * Closes the specified name-space if the transformation context is isolated and we are not in the rendering engine.
     * 
     * @param nameSpace the name-space to close
     * @param transformationContextIsolated whether the transformation context is isolated
     * @param isInRenderingEngine whether we are in the rendering engine
     */
    private void maybeCloseNameSpace(String nameSpace, boolean transformationContextIsolated,
        Object isInRenderingEngine)
    {
        // Note that we check if we are in the rendering engine as this cleanup must be done only once after the
        // document has been displayed but the display method can be called recursively. We know it's the initial entry
        // point when isInRenderingEngine is false.
        if (transformationContextIsolated && (isInRenderingEngine == null || isInRenderingEngine == Boolean.FALSE)) {
            try {
                velocityManager.getVelocityEngine().stoppedUsingMacroNamespace(nameSpace);
                logger.debug("Stopped using velocity macro namespace [{}].", nameSpace);
            } catch (Exception e) {
                // Failed to get the Velocity Engine and thus to clear Velocity Macro cache. Log this as a warning but
                // continue since it's not absolutely critical.
                logger.warn("Failed to notify Velocity Macro cache for closing the [{}] namespace. Reason = [{}]",
                    nameSpace, e.getMessage());
            }
        }
    }

    /**
     * Displays the given document in a new execution context.
     * 
     * @param document the document to display; this document is also put on the new execution context
     * @param nameSpace the name-space to be used when performing the display transformations
     * @param parameters the display parameters
     * @return the result of displaying the given document
     */
    private XDOM displayInIsolatedExecutionContext(DocumentModelBridge document, String nameSpace,
        DocumentDisplayerParameters parameters)
    {
        Map<String, Object> backupObjects = new HashMap<String, Object>();
        EntityReference currentWikiReference = this.modelContext.getCurrentEntityReference();
        try {
            // The following method call also clones the execution context.
            documentAccessBridge.pushDocumentInContext(backupObjects, document);
            // Make sure to synchronize the context wiki with the context document's wiki.
            modelContext.setCurrentEntityReference(document.getDocumentReference().getWikiReference());
            return display(document, nameSpace, parameters);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            documentAccessBridge.popDocumentFromContext(backupObjects);
            // Also restore the context wiki.
            this.modelContext.setCurrentEntityReference(currentWikiReference);
        }
    }

    /**
     * Displays the given document in the current execution context.
     * 
     * @param document the document to display
     * @param nameSpace the name-space to be used when performing the display transformations
     * @param parameters the display parameters
     * @return the result of displaying the given document
     */
    protected XDOM display(DocumentModelBridge document, String nameSpace, DocumentDisplayerParameters parameters)
    {
        // This is a clone of the cached content that can be safely modified.
        XDOM content = getContent(document, parameters);

        if (!parameters.isContentTransformed()) {
            return content;
        }

        // Before executing the XDOM transformations make sure the references used by them (e.g. the 'reference'
        // parameter of the Include macro) are resolved relative to the current document on the execution context.
        content.getMetaData().addMetaData(MetaData.BASE,
            defaultEntityReferenceSerializer.serialize(documentAccessBridge.getCurrentDocumentReference()));

        TransformationContext txContext =
            new TransformationContext(content, document.getSyntax(), parameters.isTransformationContextRestricted());
        txContext.setId(nameSpace);
        try {
            transformationManager.performTransformations(content, txContext);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return content;
    }

    /**
     * Get the content to display (either the entire document content or the content of a specific section).
     * 
     * @param document the source document
     * @param parameters the display parameters
     * @return the content as an XDOM tree
     */
    private XDOM getContent(DocumentModelBridge document, final DocumentDisplayerParameters parameters)
    {
        XDOM content = parameters.isContentTranslated() ? getTranslatedContent(document) : document.getXDOM();

        if (parameters.getSectionId() != null) {
            HeaderBlock headerBlock =
                content.getFirstBlock(new CompositeBlockMatcher(new ClassBlockMatcher(HeaderBlock.class),
                    new BlockMatcher()
                    {
                        @Override
                        public boolean match(Block block)
                        {
                            return ((HeaderBlock) block).getId().equals(parameters.getSectionId());
                        }
                    }), Block.Axes.DESCENDANT);
            if (headerBlock == null) {
                throw new RuntimeException("Cannot find section [" + parameters.getSectionId() + "] in document ["
                    + this.defaultEntityReferenceSerializer.serialize(document.getDocumentReference()) + "]");
            } else {
                content = new XDOM(headerBlock.getSection().getChildren(), content.getMetaData());
            }
        }

        return content;
    }

    /**
     * Get the translated content of the given document as XDOM tree. If the language of the given document matches the
     * context language (meaning that the given document is the current translation) then we use the content of the
     * given document (including the content changes that could have been made prior to calling this method). Otherwise
     * we load the current translation from the database/cache and use its content.
     * 
     * @param document the source document
     * @return the translated content of the given document, as XDOM tree
     */
    private XDOM getTranslatedContent(DocumentModelBridge document)
    {
        try {
            DocumentModelBridge translatedDocument =
                documentAccessBridge.getTranslatedDocumentInstance(document.getDocumentReference());
            // FIXME: This is not a reliable way to determine if the language of the given document matches the context
            // language. For instance the given document can have "en" language set while the translated document
            // returned by the document access bridge can have "" or "default" language set.
            if (!document.getRealLanguage().equals(translatedDocument.getRealLanguage())) {
                // The language of the given document doesn't match the context language. Use the translated content.
                if (document.getSyntax().equals(translatedDocument.getSyntax())) {
                    // Use getXDOM() because it caches the XDOM.
                    return translatedDocument.getXDOM();
                } else {
                    // If the translated document has a different syntax then we have to parse its content using the
                    // syntax of the given document.
                    return parseContent(translatedDocument.getContent(), document.getSyntax(),
                        document.getDocumentReference());
                }
            }
        } catch (Exception e) {
            // Use the content of the given document.
        }
        return document.getXDOM();
    }

    /**
     * Parses a string content.
     * 
     * @param content the content to parse
     * @param syntax the syntax of the given content
     * @return the result of parsing the given content
     */
    private XDOM parseContent(String content, Syntax syntax, DocumentReference source)
    {
        try {
            return parser.parse(content, syntax, source);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Makes the execution component available to the derived classes.
     * <p>
     * Note: The fact that this class uses the execution component is an implementation detail and derived classes
     * shouldn't depend on it but it seems that {@code @Inject} annotation fails to inject the execution component in
     * this class if a derived class redefines the same field.
     * 
     * @return the execution component
     */
    protected Execution getExecution()
    {
        return execution;
    }
}
