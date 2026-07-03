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
package org.xwiki.icon.internal;

import java.util.List;
import java.util.function.Supplier;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.platform.security.requiredrights.RequiredRight;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.display.BlockSupplierProvider;
import org.xwiki.rendering.block.Block;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseObjectReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link IconSetRequiredRightsAnalyzer}.
 *
 * @version $Id$
 */
@ComponentTest
class IconSetRequiredRightsAnalyzerTest
{
    private static final String CONTENT = "mocked content";

    @InjectMockComponents
    private IconSetRequiredRightsAnalyzer analyzer;

    @MockComponent
    private BlockSupplierProvider<BaseObject> objectBlockSupplierProvider;

    @MockComponent
    @Named("translation")
    private BlockSupplierProvider<String> translationBlockSupplierProvider;

    @MockComponent
    @Named("stringCode")
    private BlockSupplierProvider<String> stringCodeBlockSupplierProvider;

    @Mock
    private BaseObject baseObject;

    @Mock
    private BaseObjectReference objectReference;

    @Mock
    private DocumentReference documentReference;

    @Mock
    private XWikiDocument ownerDocument;

    @BeforeEach
    void setUp()
    {
        when(this.baseObject.getReference()).thenReturn(this.objectReference);
        when(this.baseObject.getDocumentReference()).thenReturn(this.documentReference);
        when(this.baseObject.getOwnerDocument()).thenReturn(this.ownerDocument);
        when(this.ownerDocument.getContent()).thenReturn(CONTENT);
    }

    @Test
    void analyze()
    {
        Block objectDetailsBlock = mockSupplierProvider(this.objectBlockSupplierProvider, this.baseObject);
        Block contentDetailsBlock = mockSupplierProvider(this.stringCodeBlockSupplierProvider, CONTENT);

        Block objectSummaryBlock =
            mockSupplierProvider(this.translationBlockSupplierProvider, "icon.requiredrights.object");
        Block contentSummaryBlock =
            mockSupplierProvider(this.translationBlockSupplierProvider, "icon.requiredrights.content");

        List<RequiredRightAnalysisResult> analysisResults = this.analyzer.analyze(this.baseObject);

        assertEquals(2, analysisResults.size());

        RequiredRightAnalysisResult objectResult = analysisResults.get(0);
        assertEquals(this.objectReference, objectResult.getEntityReference());
        assertEquals(objectSummaryBlock, objectResult.getSummaryMessage());
        assertEquals(objectDetailsBlock, objectResult.getDetailedMessage());
        assertEquals(List.of(RequiredRight.SCRIPT), objectResult.getRequiredRights());

        RequiredRightAnalysisResult contentResult = analysisResults.get(1);
        assertEquals(this.documentReference, contentResult.getEntityReference());
        assertEquals(contentSummaryBlock, contentResult.getSummaryMessage());
        assertEquals(contentDetailsBlock, contentResult.getDetailedMessage());
        assertEquals(RequiredRight.SCRIPT_AND_MAYBE_PROGRAM, contentResult.getRequiredRights());
    }

    private <T> Block mockSupplierProvider(BlockSupplierProvider<T> supplierProvider, T object)
    {
        Supplier<Block> objectBlockSupplier = mock();
        Block objectBlock = mock();
        when(objectBlockSupplier.get()).thenReturn(objectBlock);
        when(supplierProvider.get(object)).thenReturn(objectBlockSupplier);
        return objectBlock;
    }
}
