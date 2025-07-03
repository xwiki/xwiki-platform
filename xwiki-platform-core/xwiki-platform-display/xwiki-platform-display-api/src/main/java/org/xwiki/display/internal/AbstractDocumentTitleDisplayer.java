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

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.util.ParserUtils;
import org.xwiki.security.authorization.DocumentAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.velocity.VelocityTemplate;
import org.xwiki.velocity.XWikiVelocityException;

/**
 * Displays the title of a document.
 *
 * @version $Id$
 * @since 3.2M3
 */
public abstract class AbstractDocumentTitleDisplayer implements DocumentDisplayer
{
    /**
     * The object used for logging.
     */
    @Inject
    private Logger logger;

    /**
     * The component used to parse the rendered title into an XDOM.
     */
    @Inject
    @Named("plain/1.0")
    private Parser plainTextParser;

    /**
     * The component used to get the Velocity Engine and the Velocity Context needed to evaluate the Velocity script
     * from the document title.
     */
    @Inject
    private VelocityManager velocityManager;

    /**
     * The component used to get the current document reference.
     */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /**
     * The component used to serialize entity references.
     */
    @Inject
    private EntityReferenceSerializer<String> defaultEntityReferenceSerializer;

    @Inject
    private DocumentReferenceDequeContext documentReferenceDequeContext;

    @Inject
    @Named("xwikicfg")
    private ConfigurationSource xwikicfg;

    @Inject
    private DocumentAuthorizationManager authorizationManager;

    /**
     * Used to get the default document reference, which normally is used to represent the home page of a space.
     *
     * @see #getStaticTitle(DocumentModelBridge)
     */
    @Inject
    private EntityReferenceProvider defaultEntityReferenceProvider;

    @Inject
    private ModelContext modelContext;

    /**
     * Used to emulate an in-line parsing.
     */
    private ParserUtils parserUtils = new ParserUtils();

    @Override
    public XDOM display(DocumentModelBridge document, DocumentDisplayerParameters parameters)
    {
        // Protect against infinite recursion which can happen for instance if the document title displayer is called on
        // the current document from the title field or from a script within the first content heading.
        Deque<DocumentReference> documentReferenceStack =
            this.documentReferenceDequeContext.getDocumentReferenceDeque("title");

        if (documentReferenceStack.contains(document.getDocumentReference())) {
            this.logger.warn(
                "Infinite recursion detected while displaying the title of [{}]. Using the document name as title.",
                document.getDocumentReference());

            return getStaticTitle(document);
        }

        documentReferenceStack.push(document.getDocumentReference());
        try {
            return displayTitle(document, parameters);
        } finally {
            documentReferenceStack.pop();
        }
    }


    private XDOM displayTitle(DocumentModelBridge document, DocumentDisplayerParameters parameters)
    {
        // 1. Try to use the title provided by the user.
        String rawTitle = document.getTitle();
        if (!StringUtils.isEmpty(rawTitle)) {
            try {
                String title = rawTitle;
                // Evaluate the title only if the document is not restricted and its content's author has script
                // right, otherwise use the raw title.
                if (!document.isRestricted() && this.authorizationManager.hasAccess(Right.SCRIPT, EntityType.DOCUMENT,
                    document.getContentAuthorReference(), document.getDocumentReference())) {
                    title = evaluateTitle(document, parameters);
                }

                return parseTitle(title);
            } catch (Exception e) {
                logger.warn("Failed to interpret title of document [{}].", document.getDocumentReference(), e);
            }
        }

        // 2. Try to extract the title from the document content.
        if ("1".equals(this.xwikicfg.getProperty("xwiki.title.compatibility", "0"))) {
            try {
                XDOM title = extractTitleFromContent(document, parameters);
                if (title != null) {
                    return title;
                }
            } catch (Exception e) {
                logger.warn("Failed to extract title from content of document [{}].", document.getDocumentReference(),
                    e);
            }
        }

        // 3. The title was not specified or its evaluation failed. Use the document name as a fall-back.
        return getStaticTitle(document);
    }

    /**
     * Parses the given title as plain text and returns the generated XDOM.
     *
     * @param title the title to be parsed
     * @return the XDOM generated from parsing the title as plain text
     */
    protected XDOM parseTitle(String title)
    {
        try {
            XDOM xdom = plainTextParser.parse(new StringReader(title));
            parserUtils.removeTopLevelParagraph(xdom.getChildren());
            return xdom;
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Evaluates the Velocity script from the specified title.
     *
     * @param document the document whose title is evaluated
     * @param parameters display parameters
     * @return the result of evaluating the Velocity script from the given title
     */
    protected String evaluateTitle(DocumentModelBridge document, DocumentDisplayerParameters parameters)
    {
        StringWriter writer = new StringWriter();
        String namespace = defaultEntityReferenceSerializer.serialize(parameters.isTransformationContextIsolated()
            ? document.getDocumentReference() : documentAccessBridge.getCurrentDocumentReference());

        // Get the velocity engine
        VelocityEngine velocityEngine;
        try {
            velocityEngine = this.velocityManager.getVelocityEngine();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Execute Velocity code
        Map<String, Object> backupObjects = null;
        boolean canPop = false;
        EntityReference currentWikiReference = this.modelContext.getCurrentEntityReference();
        try {
            if (parameters.isExecutionContextIsolated()) {
                backupObjects = new HashMap<>();
                // The following method call also clones the execution context.
                documentAccessBridge.pushDocumentInContext(backupObjects, document);
                // Pop the document from the context only if the push was successful!
                canPop = true;
                // Make sure to synchronize the context wiki with the context document's wiki.
                modelContext.setCurrentEntityReference(document.getDocumentReference().getWikiReference());
            }

            VelocityTemplate preparedTitle = prepareTitle(document);

            velocityEngine.evaluate(this.velocityManager.getVelocityContext(), writer, namespace, preparedTitle);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (canPop) {
                documentAccessBridge.popDocumentFromContext(backupObjects);
                // Also restore the context wiki.
                this.modelContext.setCurrentEntityReference(currentWikiReference);
            }
        }
        return writer.toString();
    }

    private VelocityTemplate prepareTitle(DocumentModelBridge document) throws XWikiVelocityException
    {
        Object preparedTitle = document.getPreparedTitle();

        if (!(preparedTitle instanceof VelocityTemplate)) {
            preparedTitle = this.velocityManager.compile(
                this.defaultEntityReferenceSerializer.serialize(document.getDocumentReference()) + "#title",
                new StringReader(document.getTitle()));
        }

        // Remember the prepared title
        document.setPreparedTitle(preparedTitle);

        return (VelocityTemplate) preparedTitle;
    }

    /**
     * Extracts the title from the document content.
     *
     * @param document the document to extract the title from
     * @param parameters display parameters
     * @return the title XDOM
     * @deprecated since 7.0M1
     */
    @Deprecated
    protected abstract XDOM extractTitleFromContent(DocumentModelBridge document,
        DocumentDisplayerParameters parameters);

    /**
     * @param document an XWiki document
     * @return the title used as a fall-back when the dynamic title cannot be evaluated
     */
    private XDOM getStaticTitle(DocumentModelBridge document)
    {
        String documentName = document.getDocumentReference().getName();
        if (defaultEntityReferenceProvider.getDefaultReference(EntityType.DOCUMENT).getName().equals(documentName)) {
            // This document represents a space (it is the home page of a space). Use the space name instead.
            documentName = document.getDocumentReference().getParent().getName();
        }
        return parseTitle(documentName);
    }

    /**
     * @return the object used for logging
     */
    protected Logger getLogger()
    {
        return logger;
    }
}
