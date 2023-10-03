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
package org.xwiki.platform.security.requiredrights.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.validation.edit.EditConfirmationChecker;
import org.xwiki.model.validation.edit.EditConfirmationCheckerResult;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalyzer;
import org.xwiki.platform.security.requiredrights.RequiredRightsException;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.template.TemplateManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

import static javax.script.ScriptContext.GLOBAL_SCOPE;

/**
 * Check for the presence of required rights results for the current document before editing it.
 * @version $Id$
 * @since 15.9RC1
 */
@Component
@Singleton
@Named("requiredRights")
public class RequiredRightsEditConfirmationChecker implements EditConfirmationChecker
{
    @Inject
    @Named(XWikiDocumentRequiredRightAnalyzer.ID)
    private RequiredRightAnalyzer<XWikiDocument> analyzer;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private TemplateManager templateManager;

    @Inject
    private ContextualAuthorizationManager authorization;

    @Inject
    private ScriptContextManager scriptContextManager;

    @Override
    public Optional<EditConfirmationCheckerResult> check()
    {

        XWikiContext context = this.xcontextProvider.get();
        XWikiDocument tdoc = (XWikiDocument) context.get("tdoc");
        if (!this.authorization.hasAccess(Right.EDIT, tdoc.getDocumentReferenceWithLocale())) {
            return Optional.empty();
        }
        try {
            List<RequiredRightAnalysisResult> analysisResults = this.analyzer.analyze(tdoc);
            if (analysisResults.isEmpty()) {
                return Optional.empty();
            }
            var map = new HashMap<EntityReference, List<RequiredRightAnalysisResult>>();
            for (RequiredRightAnalysisResult requiredRightAnalysisResult : analysisResults) {
                EntityReference entityReference = requiredRightAnalysisResult.getEntityReference();
                if (map.containsKey(entityReference)) {
                    map.get(entityReference).add(requiredRightAnalysisResult);
                } else {
                    map.put(entityReference, new ArrayList<>(List.of(requiredRightAnalysisResult)));
                }
            }
            this.scriptContextManager.getCurrentScriptContext().setAttribute("analysis", map, GLOBAL_SCOPE);
            return Optional.of(new EditConfirmationCheckerResult(
                this.templateManager.executeNoException("security/requiredrights/requiredRightsConfirmationChecker.vm",
                    false), false));
        } catch (RequiredRightsException e) {
            throw new RuntimeException(e);
        }
    }
}
