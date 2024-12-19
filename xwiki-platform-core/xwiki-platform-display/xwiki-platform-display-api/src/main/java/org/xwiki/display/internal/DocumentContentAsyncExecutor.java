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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.context.Execution;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.RenderingException;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.HeaderBlock;
import org.xwiki.rendering.block.ImageBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.ClassBlockMatcher;
import org.xwiki.rendering.block.match.CompositeBlockMatcher;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.parser.ContentParser;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.rendering.transformation.TransformationException;
import org.xwiki.rendering.transformation.TransformationManager;
import org.xwiki.rendering.util.IdGenerator;
import org.xwiki.velocity.VelocityManager;

/**
 * Default implementation of DocumentContentAsyncRenderer.
 * 
 * @version $Id$
 * @since 11.8RC1
 */
@Component(roles = DocumentContentAsyncExecutor.class)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class DocumentContentAsyncExecutor
{
    /**
     * The context property which indicates if the current code was called from a template (only Velocity execution) or
     * from a wiki page (wiki syntax rendering).
     */
    private static final String IS_IN_RENDERING_ENGINE = "isInRenderingEngine";

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private EntityReferenceSerializer<String> defaultEntityReferenceSerializer;

    @Inject
    private Execution execution;

    @Inject
    private VelocityManager velocityManager;

    @Inject
    private ModelContext modelContext;

    @Inject
    private ContentParser parser;

    @Inject
    private TransformationManager transformationManager;

    @Inject
    private Logger logger;

    private DocumentDisplayerParameters parameters;

    private XDOM xdom;

    private DocumentReference documentReference;

    private DocumentModelBridge document;

    private Syntax syntax;

    private String transformationId;

    /**
     * @param transformationId the transformation identifier
     * @param document the document to execute
     * @param parameters display parameters
     */
    public void initialize(String transformationId, DocumentModelBridge document,
        DocumentDisplayerParameters parameters)
    {
        this.parameters = parameters;

        this.transformationId = transformationId;
        this.xdom = getPreparedContent(document, parameters);
        this.documentReference = document.getDocumentReference();
        this.syntax = document.getSyntax();
        this.document = document;
    }

    /**
     * Get the content to display (either the entire document content or the content of a specific section).
     * 
     * @param document the source document
     * @param parameters the display parameters
     * @return the content as an XDOM tree
     */
    private XDOM getPreparedContent(DocumentModelBridge document, final DocumentDisplayerParameters parameters)
    {
        XDOM content =
            parameters.isContentTranslated() ? getPreparedTranslatedContent(document) : document.getPreparedXDOM();

        if (parameters.getSectionId() != null) {
            HeaderBlock headerBlock =
                content.getFirstBlock(
                    new CompositeBlockMatcher(new ClassBlockMatcher(HeaderBlock.class),
                        block -> ((HeaderBlock) block).getId().equals(parameters.getSectionId())),
                    Block.Axes.DESCENDANT);
            if (headerBlock == null) {
                throw new RuntimeException("Cannot find section [" + parameters.getSectionId() + "] in document ["
                    + this.defaultEntityReferenceSerializer.serialize(document.getDocumentReference()) + "]");
            } else {
                content = new XDOM(headerBlock.getSection().getChildren(), content.getMetaData());
            }
        }

        IdGenerator idGenerator = parameters.getIdGenerator();
        if (idGenerator != null) {
            content.setIdGenerator(idGenerator);
            makeIdsUnique(content, idGenerator);
        }

        return content;
    }

    /**
     * Replace ids of heading and images by unique ids.
     *
     * @param content the XDOM to modify in-place
     * @param idGenerator the id generator for unique ids
     * @since 14.2RC1
     */
    private void makeIdsUnique(XDOM content, IdGenerator idGenerator)
    {
        // Traverse the XDOM and adapt all image and heading blocks.
        content.getBlocks(block -> {
            if (block instanceof ImageBlock) {
                ImageBlock imageBlock = (ImageBlock) block;
                imageBlock.setId(adaptId(idGenerator, imageBlock.getId()));
            } else if (block instanceof HeaderBlock) {
                HeaderBlock headerBlock = (HeaderBlock) block;
                headerBlock.setId(adaptId(idGenerator, headerBlock.getId()));
            }
            return false;
        }, Block.Axes.DESCENDANT);
    }

    /**
     * @param idGenerator the id generator to use
     * @param id the id to adapt to make it unique
     * @return the unique id, can be the input if was already unique
     * @since 14.2RC1
     */
    private String adaptId(IdGenerator idGenerator, String id)
    {
        if (StringUtils.isNotBlank(id)) {
            String prefix = id.substring(0, 1);
            String suffix = id.substring(1);
            return idGenerator.generateUniqueId(prefix, suffix);
        }
        return id;
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
    private XDOM getPreparedTranslatedContent(DocumentModelBridge document)
    {
        try {
            DocumentModelBridge translatedDocument = this.documentAccessBridge.getTranslatedDocumentInstance(document);

            // FIXME: This is not a reliable way to determine if the language of the given document matches the context
            // language. For instance the given document can have "en" language set while the translated document
            // returned by the document access bridge can have "" or "default" language set.
            if (!document.getRealLanguage().equals(translatedDocument.getRealLanguage())) {
                // The language of the given document doesn't match the context language. Use the translated content.
                if (document.getSyntax().equals(translatedDocument.getSyntax())) {
                    // Use getXDOM() because it caches the XDOM.
                    return translatedDocument.getPreparedXDOM();
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

        return document.getPreparedXDOM();
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
            return this.parser.parse(content, syntax, source);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param async true if the execution is asynchronous
     * @return the result of the execution
     * @throws RenderingException when failing to execute the content
     */
    public XDOM execute(boolean async) throws RenderingException
    {
        // This tells display() methods that we are inside the rendering engine and thus that they can return wiki
        // syntax and not HTML syntax (which is needed when outside the rendering engine, i.e. when we're inside
        // templates using only Velocity for example).
        Map<Object, Object> xwikiContext = getXWikiContextMap();
        Object isInRenderingEngine = xwikiContext.put(IS_IN_RENDERING_ENGINE, true);

        maybeOpenNameSpace(isInRenderingEngine);

        try {
            if (this.parameters.isExecutionContextIsolated()) {
                executeInIsolatedExecutionContext(async);
            } else {
                executeInCurrentExecutionContext(async);
            }
        } finally {
            // Since we configure Velocity to have local macros (i.e. macros visible only to the local context), since
            // Velocity caches the velocity macros in a local cache (we use key which is the absolute document
            // reference) and since documents can include other documents or panels, we need to make sure we empty the
            // local Velocity macro cache at the end of the rendering for the document as otherwise the local Velocity
            // macro caches will keep growing as users create new pages.
            maybeCloseNameSpace(isInRenderingEngine);

            if (isInRenderingEngine != null) {
                xwikiContext.put(IS_IN_RENDERING_ENGINE, isInRenderingEngine);
            } else {
                xwikiContext.remove(IS_IN_RENDERING_ENGINE);
            }
        }

        return this.xdom;
    }

    /**
     * @return the XWiki context map
     */
    @SuppressWarnings("unchecked")
    private Map<Object, Object> getXWikiContextMap()
    {
        return (Map<Object, Object>) this.execution.getContext().getProperty("xwikicontext");
    }

    /**
     * Opens the specified name-space if the transformation context is isolated and we are not in the rendering engine.
     * 
     * @param isInRenderingEngine whether we are in the rendering engine
     */
    private void maybeOpenNameSpace(Object isInRenderingEngine)
    {
        if (this.parameters.isTransformationContextIsolated()
            && (isInRenderingEngine == null || isInRenderingEngine == Boolean.FALSE)) {
            try {
                // Mark that we're starting to use a different Velocity macro name-space.
                this.velocityManager.getVelocityEngine().startedUsingMacroNamespace(this.transformationId);
                this.logger.debug("Started using velocity macro namespace [{}].", this.transformationId);
            } catch (Exception e) {
                // Failed to get the Velocity Engine and thus to clear Velocity Macro cache. Log this as a warning but
                // continue since it's not absolutely critical.
                this.logger.warn("Failed to notify Velocity Macro cache for opening the [{}] namespace. Reason = [{}]",
                    this.transformationId, ExceptionUtils.getRootCauseMessage(e));
            }
        }
    }

    /**
     * Closes the specified name-space if the transformation context is isolated and we are not in the rendering engine.
     * 
     * @param isInRenderingEngine whether we are in the rendering engine
     */
    private void maybeCloseNameSpace(Object isInRenderingEngine)
    {
        // Note that we check if we are in the rendering engine as this cleanup must be done only once after the
        // document has been displayed but the display method can be called recursively. We know it's the initial entry
        // point when isInRenderingEngine is false.
        if (this.parameters.isTransformationContextIsolated()
            && (isInRenderingEngine == null || isInRenderingEngine == Boolean.FALSE)) {
            try {
                this.velocityManager.getVelocityEngine().stoppedUsingMacroNamespace(this.transformationId);
                this.logger.debug("Stopped using velocity macro namespace [{}].", this.transformationId);
            } catch (Exception e) {
                // Failed to get the Velocity Engine and thus to clear Velocity Macro cache. Log this as a warning but
                // continue since it's not absolutely critical.
                this.logger.warn("Failed to notify Velocity Macro cache for closing the [{}] namespace. Reason = [{}]",
                    this.transformationId, e.getMessage());
            }
        }
    }

    /**
     * Displays the given document in a new execution context.
     * 
     * @return the result of displaying the given document
     * @throws RenderingException when failing to rendering the content
     */
    private void executeInIsolatedExecutionContext(boolean async) throws RenderingException
    {
        Map<String, Object> backupObjects = new HashMap<>();
        EntityReference currentWikiReference = this.modelContext.getCurrentEntityReference();
        try {
            // The following method call also clones the execution context.
            if (async || this.document == null) {
                this.documentAccessBridge.pushDocumentInContext(backupObjects, this.documentReference);
            } else {
                this.documentAccessBridge.pushDocumentInContext(backupObjects, this.document);
            }

            // Make sure to synchronize the context wiki with the context document's wiki.
            this.modelContext.setCurrentEntityReference(this.documentReference.getWikiReference());

            executeInCurrentExecutionContext(async);
        } catch (Exception e) {
            throw new RenderingException(e.getMessage(), e);
        } finally {
            this.documentAccessBridge.popDocumentFromContext(backupObjects);

            // Also restore the context wiki.
            this.modelContext.setCurrentEntityReference(currentWikiReference);
        }
    }

    /**
     * Displays the given document in the current execution context.
     * 
     * @return the result of displaying the given document
     * @throws TransformationException when failing to execute transformations
     */
    private void executeInCurrentExecutionContext(boolean async) throws RenderingException
    {
        if (!async && !this.parameters.isContentTransformed()) {
            return;
        }

        // Before executing the XDOM transformations make sure the references used by them (e.g. the 'reference'
        // parameter of the Include macro) are resolved relative to the current document on the execution context.
        this.xdom.getMetaData().addMetaData(MetaData.BASE,
            this.defaultEntityReferenceSerializer.serialize(this.documentAccessBridge.getCurrentDocumentReference()));

        TransformationContext txContext =
            new TransformationContext(this.xdom, this.syntax, this.parameters.isTransformationContextRestricted());
        txContext.setId(this.transformationId);
        txContext.setTargetSyntax(this.parameters.getTargetSyntax());
        this.transformationManager.performTransformations(this.xdom, txContext);
    }
}
