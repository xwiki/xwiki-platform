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
package org.xwiki.rendering.internal.resolver;

import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.resolver.DocumentExistenceCheckRecorder;
import org.xwiki.rendering.resolver.DocumentExistenceOverride;

/**
 * Checks the existence of documents during the resolution of a resource reference: the existence overridden by the
 * caller if any, the actual state of the document otherwise. Each check is reported to the caller's recorder, if any.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
public final class DocumentExistenceChecker
{
    private final DocumentExistenceOverride existenceOverride;

    private final DocumentExistenceCheckRecorder recorder;

    private final DocumentAccessBridge documentAccessBridge;

    private final Logger logger;

    /**
     * @param existenceOverride the existence to assume for some documents
     * @param recorder the recorder to report the checked documents to, {@code null} if none
     * @param documentAccessBridge used to check the actual existence of the documents
     * @param logger used to report the failures to check the existence of a document
     */
    public DocumentExistenceChecker(DocumentExistenceOverride existenceOverride,
        DocumentExistenceCheckRecorder recorder, DocumentAccessBridge documentAccessBridge, Logger logger)
    {
        this.existenceOverride = existenceOverride;
        this.recorder = recorder;
        this.documentAccessBridge = documentAccessBridge;
        this.logger = logger;
    }

    /**
     * @param documentReference the reference of the document
     * @return {@code true} if the document should be considered as existing
     */
    public boolean exists(DocumentReference documentReference)
    {
        if (this.recorder != null) {
            this.recorder.add(documentReference);
        }

        return this.existenceOverride.getExistence(documentReference)
            .orElseGet(() -> actuallyExists(documentReference));
    }

    private boolean actuallyExists(DocumentReference documentReference)
    {
        try {
            return this.documentAccessBridge.exists(documentReference);
        } catch (Exception e) {
            this.logger.error("Failed to check the existence of the document with reference [{}]", documentReference,
                e);
        }

        return false;
    }
}
