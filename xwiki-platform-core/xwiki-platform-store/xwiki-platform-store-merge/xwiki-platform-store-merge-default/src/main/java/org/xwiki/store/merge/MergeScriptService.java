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
package org.xwiki.store.merge;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.diff.Conflict;
import org.xwiki.diff.ConflictDecision;
import org.xwiki.diff.internal.DefaultConflictDecision;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.merge.MergeConfiguration;

/**
 * Script service allowing to perform merge operations on documents.
 *
 * @version $Id$
 * @since 11.5RC1
 */
@Component
@Named("merge")
@Singleton
public class MergeScriptService implements ScriptService
{
    /**
     * Used to lookup parsers and renderers to discover available syntaxes.
     */
    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private MergeConflictDecisionsManager conflictDecisionsManager;

    @Inject
    private MergeManager mergeManager;

    @Inject
    private ContextualAuthorizationManager contextualAuthorizationManager;

    /**
     * Perform a merge 3 points between the currentDocument and the newDocument, the previousDocument being the common
     * ancestor of both documents. This operation doesn't save anything.
     *
     * @param previousDocument the previous version of the document to merge.
     * @param currentDocument the current version of the document to merge.
     * @param newDocument the new version of the document to merge.
     * @return a {@link MergeDocumentResult} object containing the merged document.
     */
    public MergeDocumentResultScript mergeDocument(Document previousDocument, Document currentDocument,
        Document newDocument)
    {
        MergeConfiguration mergeConfiguration = new MergeConfiguration();
        mergeConfiguration.setProvidedVersionsModifiables(false);

        XWikiDocument previousDoc = previousDocument.getDocument();
        XWikiDocument currentDoc = currentDocument.getDocument();
        XWikiDocument newDoc = newDocument.getDocument();

        MergeDocumentResult mergeResult =
            mergeManager.mergeDocument(previousDoc, newDoc, currentDoc, mergeConfiguration);

        return new MergeDocumentResultScript(mergeResult, contextProvider.get(),
            this.contextualAuthorizationManager.hasAccess(Right.PROGRAM));
    }

    /**
     * Create a {@link ConflictDecision} based on an existing conflict and a choice made.
     *
     * @param conflict the conflict for which the decision has been taken.
     * @param type the choice made to solve the conflict.
     * @param customDecision if a value is given, the decision type will be custom and this value will be used to solve
     *                  the conflict.
     * @return a decision that solve the conflict.
     * @since 11.8RC1
     */
    public ConflictDecision<String> getDecision(Conflict<String> conflict, ConflictDecision.DecisionType type,
        String customDecision)
    {
        ConflictDecision<String> conflictDecision = new DefaultConflictDecision<>(conflict);
        if (StringUtils.isEmpty(customDecision)) {
            conflictDecision.setType(type);
        } else {
            conflictDecision.setCustom(Collections.singletonList(customDecision));
        }

        return conflictDecision;
    }

    /**
     * Record all the decisions taken in the {@link MergeConflictDecisionsManager}.
     *
     * @param decisionList the list of decision to save.
     * @since 11.8RC1
     */
    public void recordDecisions(List<ConflictDecision> decisionList)
    {
        XWikiContext context = contextProvider.get();
        conflictDecisionsManager.setConflictDecisionList(decisionList,
            context.getDoc().getDocumentReferenceWithLocale(), context.getUserReference());
    }
}
