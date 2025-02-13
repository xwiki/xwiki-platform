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
package org.xwiki.platform.security.requiredrights.script;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalyzer;
import org.xwiki.platform.security.requiredrights.RequiredRightsException;
import org.xwiki.platform.security.requiredrights.internal.RequiredRightChangeSuggestion;
import org.xwiki.platform.security.requiredrights.internal.RequiredRightsChangeSuggestionManager;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.requiredrights.DocumentRequiredRights;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.api.Document;

/**
 * Script service for required rights.
 *
 * @version $Id$
 * @since 16.10.0RC1
 */
@Named("security.requiredrights")
@Singleton
@Component
@Unstable
public class SecurityRequiredRightsScriptService implements ScriptService
{
    @Inject
    @Named("full")
    private RequiredRightAnalyzer<DocumentReference> fullAnalyzer;

    @Inject
    private ContextualAuthorizationManager contextualAuthorizationManager;

    @Inject
    private RequiredRightsChangeSuggestionManager rightsChangeManager;

    /**
     * Analyze the required rights of the document referenced by the given reference.
     *
     * @param documentReference the reference of the document to analyze
     * @return the list of analysis results
     * @throws AccessDeniedException if the document cannot be accessed
     * @throws RequiredRightsException if there is a problem loading or analyzing the document
     */
    public List<RequiredRightAnalysisResult> analyzeDocument(DocumentReference documentReference) throws
        AccessDeniedException, RequiredRightsException
    {
        this.contextualAuthorizationManager.checkAccess(Right.VIEW, documentReference);

        return this.fullAnalyzer.analyze(documentReference);
    }

    /**
     * Suggests operations to change the required rights of the given document.
     *
     * @param document the document to suggest operations for
     * @return the suggested operations
     * @throws AccessDeniedException if the document cannot be accessed
     * @throws RequiredRightsException if there is a problem loading or analyzing the document
     */
    public List<RequiredRightChangeSuggestion> getSuggestedOperations(Document document)
        throws AccessDeniedException, RequiredRightsException
    {
        return getSuggestedOperations(document, analyzeDocument(document.getDocumentReference()));
    }

    /**
     * Suggests operations to change the required rights of the given document.
     *
     * @param document the document to suggest operations for
     * @param requiredRightAnalysisResults the required right analysis results, to avoid re-computing them when you
     * already have them
     * @return the suggested operations
     */
    public List<RequiredRightChangeSuggestion> getSuggestedOperations(Document document,
        List<RequiredRightAnalysisResult> requiredRightAnalysisResults)
    {
        DocumentRequiredRights currentRights = document.getRequiredRights();

        return this.rightsChangeManager.getSuggestedOperations(document.getDocumentReference(), currentRights,
            requiredRightAnalysisResults);
    }

}
