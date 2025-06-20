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

import org.xwiki.extension.xar.job.diff.DiffXarJobStatus;
import org.xwiki.extension.xar.job.diff.DocumentUnifiedDiff;
import org.xwiki.job.internal.script.safe.SafeJobStatus;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.script.safe.ScriptSafeProvider;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

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
    private ContextualAuthorizationManager authorization;

    /**
     * Creates a new safe instance that wraps the given unsafe instance.
     * 
     * @param status the wrapped object
     * @param safeProvider the provider of instances safe for public scripts
     * @param authorization the component used to check access rights
     * @since 9.11.9
     * @since 10.11.1
     * @since 11.0
     */
    public SafeDiffXarJobStatus(DiffXarJobStatus status, ScriptSafeProvider<?> safeProvider,
        ContextualAuthorizationManager authorization)
    {
        super(status, safeProvider);

        this.authorization = authorization;
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
            if (this.authorization.hasAccess(Right.VIEW, documentReference)) {
                safeDocumentDiffs.add(documentDiff);
            }
        }
        return safe(safeDocumentDiffs);
    }
}
