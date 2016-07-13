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
package org.xwiki.extension.xar.internal.script;

import java.util.ArrayList;
import java.util.List;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.extension.xar.job.diff.DiffXarJobStatus;
import org.xwiki.extension.xar.job.diff.DocumentUnifiedDiff;
import org.xwiki.job.internal.script.safe.SafeJobStatus;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.script.internal.safe.ScriptSafeProvider;

/**
 * Safe version of {@link DiffXarJobStatus}.
 * 
 * @version $Id$
 * @since 7.0RC1
 */
public class SafeDiffXarJobStatus extends SafeJobStatus<DiffXarJobStatus>
{
    /**
     * The component used to check access rights.
     */
    private DocumentAccessBridge documentAccessBridge;

    /**
     * Creates a new safe instance that wraps the given unsafe instance.
     * 
     * @param status the wrapped object
     * @param safeProvider the provider of instances safe for public scripts
     * @param documentAccessBridge the component used to check access rights
     */
    public SafeDiffXarJobStatus(DiffXarJobStatus status, ScriptSafeProvider<?> safeProvider,
        DocumentAccessBridge documentAccessBridge)
    {
        super(status, safeProvider);
        this.documentAccessBridge = documentAccessBridge;
    }

    /**
     * @return the list of documents that have differences.
     */
    public List<DocumentUnifiedDiff> getDocumentDiffs()
    {
        List<DocumentUnifiedDiff> documentDiffs = getWrapped().getDocumentDiffs();
        List<DocumentUnifiedDiff> safeDocumentDiffs = new ArrayList<>();
        for (DocumentUnifiedDiff documentDiff : documentDiffs) {
            DocumentReference documentReference = documentDiff.getPreviousReference();
            if (documentReference == null) {
                documentReference = documentDiff.getNextReference();
            }
            if (this.documentAccessBridge.isDocumentViewable(documentReference)) {
                safeDocumentDiffs.add(documentDiff);
            }
        }
        return safe(safeDocumentDiffs);
    }
}
