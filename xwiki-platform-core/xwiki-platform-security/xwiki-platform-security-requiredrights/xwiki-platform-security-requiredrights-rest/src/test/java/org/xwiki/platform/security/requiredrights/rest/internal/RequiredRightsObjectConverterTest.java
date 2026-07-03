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
import java.util.Set;

import jakarta.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.platform.security.requiredrights.RequiredRight;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.requiredrights.DocumentRequiredRight;
import org.xwiki.security.authorization.requiredrights.DocumentRequiredRights;
import org.xwiki.security.requiredrights.rest.model.jaxb.AvailableRight;
import org.xwiki.security.requiredrights.rest.model.jaxb.DocumentRightsAnalysisResult;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RequiredRightsObjectConverter}.
 *
 * @version $Id$
 */
@ComponentTest
class RequiredRightsObjectConverterTest
{
    @InjectMockComponents
    private RequiredRightsObjectConverter requiredRightsObjectConverter;

    @MockComponent
    private AvailableRightsManager availableRightsManager;

    @MockComponent
    @Named("html/5.0")
    private BlockRenderer htmlRenderer;

    @MockComponent
    @Named("withtype")
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Test
    void convertToDocumentRightsAnalysisResultWithValidData()
    {
        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        DocumentRequiredRights currentRights =
            new DocumentRequiredRights(true,
                Set.of(new DocumentRequiredRight(Right.SCRIPT, EntityType.DOCUMENT)));

        RequiredRightAnalysisResult analysisResult = mock();
        when(analysisResult.getRequiredRights())
            .thenReturn(List.of(new RequiredRight(Right.SCRIPT, EntityType.DOCUMENT, true)));
        when(analysisResult.getSummaryMessage()).thenReturn(mock());
        when(analysisResult.getDetailedMessage()).thenReturn(mock());
        when(analysisResult.getEntityReference()).thenReturn(documentReference);

        when(this.availableRightsManager.computeAvailableRights(any(), eq(documentReference)))
            .thenReturn(List.of(new AvailableRight()));

        doAnswer(invocation -> {
            WikiPrinter printer = invocation.getArgument(1);
            printer.print("HTML Content");
            return null;
        }).when(this.htmlRenderer).render(any(Block.class), any());

        when(this.entityReferenceSerializer.serialize(any())).thenReturn("SerializedReference");

        DocumentRightsAnalysisResult result = this.requiredRightsObjectConverter.toDocumentRightsAnalysisResult(
            currentRights, List.of(analysisResult), documentReference);

        assertTrue(result.getCurrentRights().isEnforce());
        assertEquals(1, result.getCurrentRights().getRights().size());
        assertEquals(1, result.getAnalysisResults().size());
        assertEquals("HTML Content", result.getAnalysisResults().get(0).getSummaryMessageHTML());
        assertEquals("HTML Content", result.getAnalysisResults().get(0).getDetailedMessageHTML());
        assertEquals("SerializedReference", result.getAnalysisResults().get(0).getEntityReference());
        assertEquals(1, result.getAvailableRights().size());
    }

    @Test
    void convertToDocumentRightsAnalysisResultWithNullValues()
    {
        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        DocumentRequiredRights currentRights = new DocumentRequiredRights(false, Set.of());

        RequiredRightAnalysisResult analysisResult = mock();
        when(analysisResult.getRequiredRights()).thenReturn(List.of());
        when(analysisResult.getSummaryMessage()).thenReturn(null);
        when(analysisResult.getDetailedMessage()).thenReturn(null);
        when(analysisResult.getEntityReference()).thenReturn(null);

        when(this.availableRightsManager.computeAvailableRights(any(), eq(documentReference)))
            .thenReturn(List.of());

        DocumentRightsAnalysisResult result = this.requiredRightsObjectConverter.toDocumentRightsAnalysisResult(
            currentRights, List.of(analysisResult), documentReference);

        assertFalse(result.getCurrentRights().isEnforce());
        assertTrue(result.getCurrentRights().getRights().isEmpty());
        assertEquals(1, result.getAnalysisResults().size());
        assertEquals("", result.getAnalysisResults().get(0).getSummaryMessageHTML());
        assertEquals("", result.getAnalysisResults().get(0).getDetailedMessageHTML());
        assertNull(result.getAnalysisResults().get(0).getEntityReference());
        assertTrue(result.getAvailableRights().isEmpty());
    }
}
