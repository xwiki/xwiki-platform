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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.validation.edit.EditConfirmationChecker;
import org.xwiki.model.validation.edit.EditConfirmationCheckerResult;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalyzer;
import org.xwiki.platform.security.requiredrights.RequiredRightsException;
import org.xwiki.platform.security.requiredrights.internal.configuration.RequiredRightsConfiguration;
import org.xwiki.platform.security.requiredrights.internal.configuration.RequiredRightsConfiguration.RequiredRightDocumentProtection;
import org.xwiki.platform.security.requiredrights.internal.editconfirmationchecker.RequiredRightAnalysisResultSkipValue;
import org.xwiki.platform.security.requiredrights.internal.editconfirmationchecker.RequiredRightsChangedResultSkipValue;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.template.TemplateManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

import static com.xpn.xwiki.doc.XWikiDocument.CKEY_TDOC;
import static javax.script.ScriptContext.GLOBAL_SCOPE;
import static org.xwiki.platform.security.requiredrights.internal.configuration.RequiredRightsConfiguration.RequiredRightDocumentProtection.NONE;
import static org.xwiki.security.authorization.Right.EDIT;

/**
 * Check for the presence of required rights results for the current document before editing it.
 *
 * @version $Id$
 * @since 15.9RC1
 */
@Component
@Singleton
@Named("requiredRights")
public class RequiredRightsEditConfirmationChecker implements EditConfirmationChecker
{
    @Inject
    private RequiredRightAnalyzer<XWikiDocument> analyzer;

    @Inject
    private RequiredRightsChangedFilter requiredRightsChangedFilter;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private TemplateManager templateManager;

    @Inject
    private ContextualAuthorizationManager authorization;

    @Inject
    private ScriptContextManager scriptContextManager;

    @Inject
    private RequiredRightsConfiguration requiredRightsConfiguration;

    @Inject
    @Named("html/5.0")
    private BlockRenderer htmlBlockRender;

    @Override
    public Optional<EditConfirmationCheckerResult> check()
    {
        Optional<EditConfirmationCheckerResult> checkResult;
        RequiredRightDocumentProtection documentProtection = this.requiredRightsConfiguration.getDocumentProtection();
        // Do nothing if the protection is deactivated.
        if (documentProtection == NONE) {
            checkResult = Optional.empty();
        } else {
            XWikiContext context = this.xcontextProvider.get();
            XWikiDocument tdoc = (XWikiDocument) context.get(CKEY_TDOC);
            // Do nothing if the current user does not have edit rights, or if the document is new.
            if (!this.authorization.hasAccess(EDIT, tdoc.getDocumentReference()) || tdoc.isNew()) {
                checkResult = Optional.empty();
            } else {
                try {
                    RequiredRightsChangedResult analysisResults =
                        this.requiredRightsChangedFilter.filter(tdoc.getAuthors(), this.analyzer.analyze(tdoc));
                    // Do nothing if the analysis does not produce results relevant for the current user.
                    if (analysisResults.isEmpty()) {
                        checkResult = Optional.empty();
                    } else {
                        this.scriptContextManager.getCurrentScriptContext()
                            .setAttribute("analysisResults", analysisResults, GLOBAL_SCOPE);
                        XDOM message = this.templateManager
                            .executeNoException("security/requiredrights/requiredRightsEditConfirmationChecker.vm");
                        checkResult = Optional.of(new EditConfirmationCheckerResult(message, false,
                            getRequiredRightsChangedSkipValue(analysisResults)));
                    }
                } catch (RequiredRightsException e) {
                    this.scriptContextManager.getCurrentScriptContext().setAttribute("exception", e, GLOBAL_SCOPE);
                    // Display an error message in case of exception.
                    XDOM message = this.templateManager
                        .executeNoException("security/requiredrights/requiredRightsEditConfirmationCheckerError.vm");
                    checkResult = Optional.of(new EditConfirmationCheckerResult(message, false));
                }
            }
        }
        return checkResult;
    }

    /**
     * Copy a {@link RequiredRightsChangedResult} to produce a serializable {@link RequiredRightsChangedResultSkipValue}
     * to use as the skip value.
     *
     * @param analysisResults the analysis result to copy
     * @return the serializable skip value
     */
    private RequiredRightsChangedResultSkipValue getRequiredRightsChangedSkipValue(
        RequiredRightsChangedResult analysisResults)
    {
        return new RequiredRightsChangedResultSkipValue(analysisResults.getAddedRights(),
            analysisResults.getRemovedRights(), convertToString(analysisResults.getAdded()),
            convertToString(analysisResults.getRemoved()));
    }

    /**
     * Copy a set of  {@link RequiredRightAnalysisResult} to a list of serializable
     * {@link RequiredRightAnalysisResultSkipValue}, later contained in a {@link RequiredRightsChangedResultSkipValue},
     * to use a the skip value.
     *
     * @param results the list result to copy
     * @return the serializable skip value
     */
    private List<RequiredRightAnalysisResultSkipValue> convertToString(Set<RequiredRightAnalysisResult> results)
    {
        return results.stream()
            .map(it -> new RequiredRightAnalysisResultSkipValue(
                it.getEntityReference(),
                it.getRequiredRights(),
                render(it.getSummaryMessage()), render(it.getDetailedMessage()
            ))).collect(Collectors.toList());
    }

    private String render(Block detailedMessage)
    {
        DefaultWikiPrinter printer = new DefaultWikiPrinter();
        this.htmlBlockRender.render(detailedMessage, printer);
        return printer.toString();
    }
}
