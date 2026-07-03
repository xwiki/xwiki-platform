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
package org.xwiki.platform.security.requiredrights.ui;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Priority;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalyzer;
import org.xwiki.platform.security.requiredrights.internal.RequiredRightChangeSuggestion;
import org.xwiki.platform.security.requiredrights.internal.RequiredRightsChangeSuggestionManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.CompositeBlock;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.requiredrights.DocumentRequiredRights;
import org.xwiki.security.authorization.requiredrights.DocumentRequiredRightsManager;
import org.xwiki.skinx.SkinExtension;
import org.xwiki.template.TemplateManager;
import org.xwiki.uiextension.UIExtension;

import com.xpn.xwiki.XWikiContext;

/**
 * Displays a warning above the content if required rights are missing.
 *
 * @version $Id$
 * @since 17.4.0RC1
 */
@Component
@Named(MissingRequiredRightWarningUIExtension.ROLE_HINT)
// This priority orders the UI extension as the used UIXP doesn't explicitly sort the UIX.
// Use a lower number than the default (1000) to have the warning before standard UI extensions that might care more
// about being close to the content.
@Priority(900)
@Singleton
public class MissingRequiredRightWarningUIExtension implements UIExtension
{
    /**
     * The role hint.
     */
    public static final String ROLE_HINT =
        "org.xwiki.platform.security.requiredrights.ui.MissingRequiredRightWarningUIExtension";

    @Inject
    private TemplateManager templateManager;

    @Inject
    private AuthorizationManager authorizationManager;

    @Inject
    private RequiredRightsChangeSuggestionManager suggestionManager;

    @Inject
    @Named("withTranslations")
    private RequiredRightAnalyzer<DocumentReference> requiredRightAnalyzer;

    @Inject
    private DocumentRequiredRightsManager documentRequiredRightsManager;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private Logger logger;

    @Inject
    @Named("jsrx")
    private SkinExtension jsrx;

    @Inject
    @Named("ssrx")
    private SkinExtension ssrx;

    @Override
    public Block execute()
    {
        XWikiContext xWikiContext = this.contextProvider.get();
        DocumentReference documentReference = xWikiContext.getDoc().getDocumentReference();
        DocumentReference userReference = xWikiContext.getUserReference();
        if ("view".equals(xWikiContext.getAction()) && this.authorizationManager.hasAccess(Right.EDIT,
            userReference, documentReference))
        {
            Block container = new GroupBlock(Map.of("id", "missing-required-rights-warning"));
            // Load the JavaScript for updating the warning when the document is saved.
            this.jsrx.use("js/security/requiredrights/requiredRightsInformationUpdater.js");
            Optional<RequiredRightChangeSuggestion> changeSuggestion = getDefinitelyMissingRequiredRight(xWikiContext);
            if (changeSuggestion.isPresent() && this.authorizationManager.hasAccess(
                changeSuggestion.get().rightToAdd().right(),
                userReference,
                documentReference.extractReference(changeSuggestion.get().rightToAdd().scope())))
            {
                this.jsrx.use("js/security/requiredrights/requiredRightsDialog.js");
                this.ssrx.use("css/security/requiredrights/requiredRightsDialog.css");
                container.addChild(
                    this.templateManager.executeNoException("security/requiredrights/missingRequiredRightWarning.vm"));
            }

            return container;
        }

        return new CompositeBlock();
    }

    private Optional<RequiredRightChangeSuggestion> getDefinitelyMissingRequiredRight(XWikiContext context)
    {
        DocumentReference documentReference = context.getDoc().getDocumentReference();

        try {
            Optional<DocumentRequiredRights> requiredRights =
                this.documentRequiredRightsManager.getRequiredRights(documentReference);

            // Only warn about missing required rights when they are enforced.
            if (requiredRights.isPresent() && requiredRights.get().enforce()) {
                List<RequiredRightAnalysisResult> analysisResults =
                    this.requiredRightAnalyzer.analyze(documentReference);
                return this.suggestionManager.getSuggestedOperations(documentReference, requiredRights.get(),
                        analysisResults)
                    .stream()
                    .filter(changeSuggestion -> changeSuggestion.increasesRights()
                        && !changeSuggestion.requiresManualReview())
                    .findFirst();
            }
        } catch (Exception e) {
            // Log the exception so admins can see them, but just don't display any warning as there is no need to
            // annoy users with a warning.
            this.logger.warn("Error getting or analyzing required rights for document [{}], root cause: [{}]",
                documentReference, ExceptionUtils.getRootCauseMessage(e));
        }
        return Optional.empty();
    }

    @Override
    public String getExtensionPointId()
    {
        return "org.xwiki.platform.template.content.header.after";
    }

    @Override
    public String getId()
    {
        return ROLE_HINT;
    }

    @Override
    public Map<String, String> getParameters()
    {
        return Map.of();
    }
}
