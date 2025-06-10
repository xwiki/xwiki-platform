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
package org.xwiki.platform.security.requiredrights.rest.internal;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.AbstractLocalizedEntityReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.security.requiredrights.rest.model.jaxb.AvailableRight;
import org.xwiki.security.requiredrights.rest.model.jaxb.DocumentRequiredRight;
import org.xwiki.security.requiredrights.rest.model.jaxb.DocumentRequiredRights;
import org.xwiki.security.requiredrights.rest.model.jaxb.DocumentRightsAnalysisResult;
import org.xwiki.security.requiredrights.rest.model.jaxb.ObjectFactory;
import org.xwiki.security.requiredrights.rest.model.jaxb.RequiredRight;
import org.xwiki.security.requiredrights.rest.model.jaxb.RequiredRightAnalysisResult;

/**
 * Convert required rights objects to the REST API data types.
 *
 * @version $Id$
 * @since 17.4.0RC1
 */
@Component(roles = RequiredRightsObjectConverter.class)
@Singleton
public class RequiredRightsObjectConverter
{
    @Inject
    @Named("html/5.0")
    private BlockRenderer htmlRenderer;

    @Inject
    @Named("withtype")
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    private AvailableRightsManager availableRightsManager;

    private final ObjectFactory factory = new ObjectFactory();

    /**
     * Convert the required rights objects to the REST API data types.
     *
     * @param currentRights the current required rights of the document
     * @param analysisResults the result of the required rights analysis
     * @param documentReference the reference of the considered document
     * @return the REST API data type
     */
    public DocumentRightsAnalysisResult toDocumentRightsAnalysisResult(
        org.xwiki.security.authorization.requiredrights.DocumentRequiredRights currentRights,
        List<org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult> analysisResults,
        DocumentReference documentReference)
    {
        org.xwiki.security.requiredrights.rest.model.jaxb.DocumentRequiredRights jaxbDocumentRights =
            convertDocumentRequiredRights(currentRights);

        List<AvailableRight> availableRights =
            this.availableRightsManager.computeAvailableRights(analysisResults, documentReference);

        List<org.xwiki.security.requiredrights.rest.model.jaxb.RequiredRightAnalysisResult> jaxbAnalysisResults =
            convertRequiredRightAnalysisResults(analysisResults);

        return this.factory.createDocumentRightsAnalysisResult()
            .withAnalysisResults(jaxbAnalysisResults)
            .withCurrentRights(jaxbDocumentRights)
            .withAvailableRights(availableRights);
    }

    private List<RequiredRightAnalysisResult> convertRequiredRightAnalysisResults(
        List<org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult> analysisResults)
    {
        return analysisResults.stream()
            .map(analysisResult -> {
                String summaryHTML = getHTML(analysisResult.getSummaryMessage());
                String detailsHTML = getHTML(analysisResult.getDetailedMessage());

                String locale;
                if (analysisResult.getEntityReference()
                    instanceof AbstractLocalizedEntityReference localizedEntityReference
                    && localizedEntityReference.getLocale() != null)
                {
                    locale = localizedEntityReference.getLocale().toString();
                } else {
                    locale = null;
                }
                return this.factory.createRequiredRightAnalysisResult()
                    .withSummaryMessageHTML(summaryHTML)
                    .withDetailedMessageHTML(detailsHTML)
                    .withEntityReference(
                        this.entityReferenceSerializer.serialize(analysisResult.getEntityReference()))
                    .withLocale(locale)
                    .withRequiredRights(
                        analysisResult.getRequiredRights().stream().map(this::mapRequiredRight).toList());
            })
            .toList();
    }

    /**
     * Converts the provided {@link org.xwiki.security.authorization.requiredrights.DocumentRequiredRights}
     * instance to a new instance of {@link DocumentRequiredRights}.
     *
     * @param currentRights the current required rights of the document to be converted
     * @return a new {@link DocumentRequiredRights} instance populated with the data from the given input
     */
    public DocumentRequiredRights convertDocumentRequiredRights(
        org.xwiki.security.authorization.requiredrights.DocumentRequiredRights currentRights)
    {
        return this.factory.createDocumentRequiredRights()
            .withEnforce(currentRights.enforce())
            .withRights(
                currentRights.rights()
                    .stream()
                    .map(this::mapDocumentRight)
                    .toList()
            );
    }

    private DocumentRequiredRight mapDocumentRight(
        org.xwiki.security.authorization.requiredrights.DocumentRequiredRight right)
    {
        if (right == null) {
            return null;
        } else {
            return this.factory.createDocumentRequiredRight()
                .withRight(right.right().toString())
                .withScope(right.scope() != null ? right.scope().toString() : null);
        }
    }

    private RequiredRight mapRequiredRight(
        org.xwiki.platform.security.requiredrights.RequiredRight right)
    {
        return this.factory.createRequiredRight()
            .withRight(right.getRight().toString())
            .withEntityType(right.getEntityType() != null ? right.getEntityType().toString() : null)
            .withManualReviewNeeded(right.isManualReviewNeeded());
    }

    private String getHTML(Block block)
    {
        WikiPrinter printer = new DefaultWikiPrinter();
        this.htmlRenderer.render(block, printer);
        return printer.toString();
    }
}
