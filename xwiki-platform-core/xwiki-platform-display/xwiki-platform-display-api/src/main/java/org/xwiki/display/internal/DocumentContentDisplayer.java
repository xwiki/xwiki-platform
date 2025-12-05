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

import java.util.Arrays;
import java.util.Deque;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.async.internal.AsyncRendererConfiguration;
import org.xwiki.rendering.async.internal.block.BlockAsyncRendererExecutor;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;

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
     * The number of recursive displays of a single document that are allowed until we stop. We need this to be at
     * least two when a document with a sheet displays the content, as it is the case in App Within Minutes.
     * Set it to five to be sure that it is enough.
     */
    private static final int INCLUSION_LIMIT = 5;

    @Inject
    private Provider<DocumentContentAsyncRenderer> rendererProvider;

    @Inject
    private BlockAsyncRendererExecutor executor;

    @Inject
    private DocumentReferenceDequeContext documentReferenceDequeContext;

    @Inject
    private Logger logger;

    @Override
    public XDOM display(DocumentModelBridge document, DocumentDisplayerParameters parameters)
    {
        Deque<DocumentReference> documentDeque =
            this.documentReferenceDequeContext.getDocumentReferenceDeque("content");

        if (countMatchingReferences(document, documentDeque) >= INCLUSION_LIMIT) {
            this.logger.warn("Infinite recursion of document content detected in [{}].",
                document.getDocumentReference());
            throw new RuntimeException("Infinite document inclusion detected.");
        }

        documentDeque.push(document.getDocumentReference());
        try {
            // Get a renderer
            DocumentContentAsyncRenderer renderer = this.rendererProvider.get();
            Set<String> contextEntries = renderer.initialize(document, parameters);

            // Configure
            AsyncRendererConfiguration configuration = new AsyncRendererConfiguration();
            configuration.setContextEntries(contextEntries);

            // Execute
            Block block = this.executor.execute(renderer, configuration);

            return block instanceof XDOM ? (XDOM) block : new XDOM(Arrays.asList(block));
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            documentDeque.pop();
        }
    }

    private static long countMatchingReferences(DocumentModelBridge document, Deque<DocumentReference> documentDeque)
    {
        DocumentReference documentReference = document.getDocumentReference();
        return documentDeque.stream().filter(ref -> ref.equals(documentReference)).count();
    }
}
