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
package org.xwiki.platform.security.requiredrights.internal.analyzer;

import java.util.List;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.platform.security.requiredrights.RequiredRight;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightsException;
import org.xwiki.platform.security.requiredrights.internal.provider.BlockSupplierProvider;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.objects.BaseObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TranslationDocumentObjectRequiredRightAnalyzer}.
 *
 * @version $Id$
 */
@ComponentTest
class TranslationDocumentObjectRequiredRightAnalyzerTest
{
    @InjectMockComponents
    private TranslationDocumentObjectRequiredRightAnalyzer analyzer;

    @MockComponent
    @Named("translation")
    private BlockSupplierProvider<String> translationMessageSupplierProvider;

    @MockComponent
    private BlockSupplierProvider<BaseObject> xObjectDisplayerProvider;

    @Mock
    BaseObject object;

    @Mock
    ObjectReference objectReference;

    @BeforeEach
    void setup()
    {
        doReturn(this.objectReference).when(this.object).getReference();
    }

    @ParameterizedTest
    @CsvSource(value = {"GLOBAL, programming", "WIKI, admin", "USER, script" })
    void analyze(String scope, Right requiredRight) throws RequiredRightsException
    {
        when(this.object.getStringValue("scope")).thenReturn(scope);

        List<RequiredRightAnalysisResult> analysisResults = this.analyzer.analyze(this.object);

        assertEquals(1, analysisResults.size());
        RequiredRightAnalysisResult analysisResult = analysisResults.get(0);
        assertEquals(this.objectReference, analysisResult.getEntityReference());
        verify(this.translationMessageSupplierProvider).get("security.requiredrights.object.translationDocument."
            + scope.toLowerCase());
        verify(this.xObjectDisplayerProvider).get(this.object);

        assertEquals(1, analysisResult.getRequiredRights().size());
        RequiredRight requiredRightResult = analysisResult.getRequiredRights().get(0);
        assertEquals(requiredRight, requiredRightResult.getRight());
        assertEquals(EntityType.DOCUMENT, requiredRightResult.getEntityType());
        assertFalse(requiredRightResult.isManualReviewNeeded());
    }
}
