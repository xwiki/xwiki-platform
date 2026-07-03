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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.localization.Translation;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalyzer;
import org.xwiki.platform.security.requiredrights.internal.RequiredRightChangeSuggestion;
import org.xwiki.platform.security.requiredrights.internal.RequiredRightsChangeSuggestionManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.BulletedListBlock;
import org.xwiki.rendering.block.CompositeBlock;
import org.xwiki.rendering.block.ListItemBlock;
import org.xwiki.rendering.block.ParagraphBlock;
import org.xwiki.rendering.block.RawBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.requiredrights.DocumentRequiredRights;
import org.xwiki.security.authorization.requiredrights.DocumentRequiredRightsManager;
import org.xwiki.skinx.SkinExtension;
import org.xwiki.uiextension.UIExtension;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserPropertiesResolver;
import org.xwiki.user.UserType;

import com.xpn.xwiki.XWikiContext;

/**
 * Displays information about the required rights in the document information.
 *
 * @version $Id$
 * @since 17.4.0RC1
 */
@Component
@Named(RequiredRightsInfoUIExtension.ROLE_HINT)
@Singleton
public class RequiredRightsInfoUIExtension implements UIExtension
{
    /**
     * The role hint.
     */
    public static final String ROLE_HINT =
        "org.xwiki.platform.security.requiredrights.ui.RequiredRightsInfoUIExtension";

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
    private ContextualLocalizationManager localizationManager;

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

    @Inject
    private UserPropertiesResolver userPropertiesResolver;

    @Override
    public Block execute()
    {
        XWikiContext xWikiContext = this.contextProvider.get();

        DocumentReference documentReference = xWikiContext.getDoc().getDocumentReference();
        DocumentReference userReference = xWikiContext.getUserReference();

        try {
            Optional<DocumentRequiredRights> requiredRightsOptional =
                this.documentRequiredRightsManager.getRequiredRights(documentReference);

            // Only display required rights for actually existing pages.
            if (requiredRightsOptional.isPresent()) {
                // Load the JavaScript for updating the information when the document is saved.
                this.jsrx.use("js/security/requiredrights/requiredRightsInformationUpdater.js");

                List<Block> results = new ArrayList<>();
                results.add(new RawBlock("<dt><label>", Syntax.HTML_5_0));
                results.add(
                    this.localizationManager.getTranslation("security.requiredrights.ui.informationLabel").render());
                results.add(new RawBlock("</label></dt><dd class=\"required-rights-information\">", Syntax.HTML_5_0));

                DocumentRequiredRights requiredRights = requiredRightsOptional.get();
                results.addAll(getCurrentRequiredrightsDisplay(requiredRights));

                boolean isAdvanced =
                    this.userPropertiesResolver.resolve(CurrentUserReference.INSTANCE).getType() == UserType.ADVANCED;

                // Display the suggested operation and the button if the user has at least edit right and is either
                // advanced or has script right.
                if (this.authorizationManager.hasAccess(Right.EDIT, userReference, documentReference)
                    && (isAdvanced || this.authorizationManager.hasAccess(Right.SCRIPT, userReference,
                    documentReference)))
                {
                    if (!requiredRights.enforce()) {
                        results.add(getTranslatedParagraph("security.requiredrights.ui.suggestEnforcing"));
                    } else {
                        // Display only the "top" operation that is suggested.
                        Optional<RequiredRightChangeSuggestion> suggestedOperationOptional =
                            getSuggestedOperation(xWikiContext);
                        suggestedOperationOptional
                            .flatMap(suggestedOperation ->
                                getSuggestionDisplay(suggestedOperation, userReference, documentReference)
                            )
                            .ifPresent(results::add);
                    }

                    // Load the CSS and JavaScript for the dialog.
                    this.jsrx.use("js/security/requiredrights/requiredRightsDialog.js");
                    this.ssrx.use("css/security/requiredrights/requiredRightsDialog.css");

                    results.add(new RawBlock("<button type=\"button\" class=\"btn btn-default\" disabled "
                        + "data-xwiki-requiredrights-dialog=\"show\">", Syntax.HTML_5_0));
                    results.add(this.localizationManager.getTranslation(
                        "security.requiredrights.ui.reviewRequiredRightsButton").render());
                    results.add(new RawBlock("</button>", Syntax.HTML_5_0));
                }
                results.add(new RawBlock("</dd>", Syntax.HTML_5_0));

                return new CompositeBlock(results);
            }
        } catch (Exception e) {
            // Log the exception so admins can see them.
            this.logger.warn("Error getting required rights for document [{}], root cause: [{}]",
                documentReference, ExceptionUtils.getRootCauseMessage(e));
        }

        return new CompositeBlock();
    }

    private Optional<Block> getSuggestionDisplay(RequiredRightChangeSuggestion suggestedOperation,
        DocumentReference userReference,
        DocumentReference documentReference)
    {
        Optional<Block> suggestionDisplay = Optional.empty();
        if (suggestedOperation.increasesRights()
            && this.authorizationManager.hasAccess(suggestedOperation.rightToAdd().right(),
            userReference,
            documentReference.extractReference(suggestedOperation.rightToAdd().scope())))
        {
            if (suggestedOperation.requiresManualReview()) {
                suggestionDisplay = Optional.of(
                    getTranslatedParagraph("security.requiredrights.ui.maybeMissingRequiredRight"));
            } else {
                suggestionDisplay = Optional.of(
                    getTranslatedParagraph("security.requiredrights.ui.missingRequiredRightsWarning"));
            }
        } else if (!suggestedOperation.increasesRights()) {
            if (suggestedOperation.requiresManualReview()) {
                suggestionDisplay = Optional.of(
                    getTranslatedParagraph("security.requiredrights.ui.maybeTooManyRequiredRights"));
            } else {
                suggestionDisplay = Optional.of(
                    getTranslatedParagraph("security.requiredrights.ui.tooManyRequiredRights"));
            }
        }
        return suggestionDisplay;
    }

    private List<Block> getCurrentRequiredrightsDisplay(DocumentRequiredRights requiredRights)
    {
        List<Block> currentRequiredRights;
        if (!requiredRights.enforce()) {
            currentRequiredRights =
                List.of(getTranslatedParagraph("security.requiredrights.ui.notEnforced"));
        } else if (requiredRights.rights().isEmpty()) {
            currentRequiredRights =
                List.of(getTranslatedParagraph("security.requiredrights.ui.enforcedNoRight"));
        } else {
            currentRequiredRights = List.of(
                getTranslatedParagraph("security.requiredrights.ui.enforced"),
                new BulletedListBlock(
                    requiredRights.rights().stream()
                        .<Block>map(right -> new ListItemBlock(List.of(
                            Optional.ofNullable(this.localizationManager.getTranslation(
                                    "security.requiredrights.ui.right." + right.right()))
                                .map(Translation::render).orElse(new WordBlock(right.right().toString()))
                        )))
                        .toList())
            );
        }
        return currentRequiredRights;
    }

    private ParagraphBlock getTranslatedParagraph(String key)
    {
        return new ParagraphBlock(List.of(this.localizationManager.getTranslation(
            key
        ).render()));
    }

    private Optional<RequiredRightChangeSuggestion> getSuggestedOperation(XWikiContext context)
    {
        DocumentReference documentReference = context.getDoc().getDocumentReference();
        Optional<RequiredRightChangeSuggestion> result = Optional.empty();

        try {
            Optional<DocumentRequiredRights> requiredRights =
                this.documentRequiredRightsManager.getRequiredRights(documentReference);

            if (requiredRights.isPresent()) {
                List<RequiredRightAnalysisResult> analysisResults =
                    this.requiredRightAnalyzer.analyze(documentReference);

                List<RequiredRightChangeSuggestion> suggestedOperations =
                    this.suggestionManager.getSuggestedOperations(documentReference, requiredRights.get(),
                        analysisResults);

                for (RequiredRightChangeSuggestion suggestedOperation : suggestedOperations) {
                    if (result.isEmpty() || (!result.get().increasesRights() && suggestedOperation.increasesRights())) {
                        result = Optional.of(suggestedOperation);
                    } else if (result.get().increasesRights() == suggestedOperation.increasesRights()
                        && result.get().requiresManualReview() && !suggestedOperation.requiresManualReview())
                    {
                        result = Optional.of(suggestedOperation);
                    }
                }
            }
        } catch (Exception e) {
            // Log the exception so admins can see them, but just don't display any warning as there is no need to
            // annoy users with a warning.
            this.logger.warn("Error getting or analyzing required rights for document [{}], root cause: [{}]",
                documentReference, ExceptionUtils.getRootCauseMessage(e));
        }

        return result;
    }

    @Override
    public String getExtensionPointId()
    {
        return "org.xwiki.platform.template.information";
    }

    @Override
    public String getId()
    {
        return ROLE_HINT;
    }

    @Override
    public Map<String, String> getParameters()
    {
        // Use an order of 600 that is a natural continuation of the "core" extensions in the left column that have
        // 100-500 in steps of 100.
        return Map.of("order", "600");
    }
}
