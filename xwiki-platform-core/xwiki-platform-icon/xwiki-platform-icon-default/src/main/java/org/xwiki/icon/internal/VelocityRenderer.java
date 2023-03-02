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
package org.xwiki.icon.internal;

import java.io.StringWriter;
import java.util.concurrent.Callable;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.velocity.VelocityContext;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.bridge.internal.DocumentContextExecutor;
import org.xwiki.component.annotation.Component;
import org.xwiki.icon.IconException;
import org.xwiki.logging.LoggerConfiguration;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.AuthorExecutor;
import org.xwiki.user.UserReferenceSerializer;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.velocity.XWikiVelocityContext;

/**
 * Internal helper to render safely any velocity code.
 *
 * @version $Id$
 * @since 6.4M1
 */
@Component(roles = VelocityRenderer.class)
@Singleton
public class VelocityRenderer
{
    private static final String NAMESPACE = "DefaultIconRenderer";

    @Inject
    private VelocityManager velocityManager;

    @Inject
    private LoggerConfiguration loggerConfiguration;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private AuthorExecutor authorExecutor;

    @Inject
    private DocumentContextExecutor documentContextExecutor;

    @Inject
    @Named("document")
    private UserReferenceSerializer<DocumentReference> documentUserSerializer;

    /**
     * Render a velocity code without messing with the document context and namespace.
     * @param code code to render
     * @param contextDocumentReference the reference of the context document
     * @return the rendered code
     * @throws IconException if problem occurs
     */
    public String render(String code, DocumentReference contextDocumentReference) throws IconException
    {
        // The macro namespace to use by the velocity engine, see afterwards.
        String namespace = "IconVelocityRenderer_" + Thread.currentThread().getId();

        // Create the output writer
        StringWriter output = new StringWriter();

        VelocityEngine engine = null;

        boolean result;

        try {
            // Get the velocity engine
            engine = this.velocityManager.getVelocityEngine();

            // Use a new macro namespace to prevent the code redefining existing macro.
            // We use the thread name to have a unique id.
            engine.startedUsingMacroNamespace(namespace);

            DocumentReference authorReference;
            DocumentModelBridge sourceDocument;

            // Execute the Velocity code in an isolated execution context with the rights of its author when the icon
            // theme is from a document.
            if (contextDocumentReference != null) {
                sourceDocument =
                    this.documentAccessBridge.getDocumentInstance(contextDocumentReference);
                authorReference = this.documentUserSerializer.serialize(sourceDocument.getAuthors().getContentAuthor());
            } else {
                authorReference = null;
                sourceDocument = null;
            }

            // Create a new VelocityContext to prevent the code creating variables in the current context.
            // See https://jira.xwiki.org/browse/XWIKI-11400.
            // We set the current context as inner context of the new one to be able to read existing variables.
            // See https://jira.xwiki.org/browse/XWIKI-11426.
            VelocityContext context = new XWikiVelocityContext(this.velocityManager.getVelocityContext(),
                this.loggerConfiguration.isDeprecatedLogEnabled());

            // Render the code
            VelocityEngine finalEngine = engine;
            Callable<Boolean> callable = () -> finalEngine.evaluate(context, output, NAMESPACE, code);
            if (contextDocumentReference != null) {
                // Wrap the callable in a document context and author executor to ensure that the document is in
                // context and the Velocity code is executed with the author's rights.
                Callable<Boolean> innerCallable = callable;
                callable = () -> this.documentContextExecutor.call(
                    () -> this.authorExecutor.call(innerCallable, authorReference, contextDocumentReference),
                    sourceDocument);
            }
            result = callable.call();
        } catch (Exception e) {
            throw new IconException("Failed to render the icon.", e);
        } finally {
            // Do not forget to close the macro namespace we have created previously
            if (engine != null) {
                engine.stoppedUsingMacroNamespace(namespace);
            }
        }

        if (result) {
            return output.toString();
        } else {
            // I don't know how to check the velocity runtime log
            throw new IconException("Failed to render the icon. See the Velocity runtime log.");
        }
    }
}
