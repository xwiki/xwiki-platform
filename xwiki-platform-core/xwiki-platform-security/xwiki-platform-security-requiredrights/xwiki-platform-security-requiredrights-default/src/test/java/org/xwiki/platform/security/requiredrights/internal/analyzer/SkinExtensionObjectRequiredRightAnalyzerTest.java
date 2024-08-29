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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.platform.security.requiredrights.RequiredRight;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightsException;
import org.xwiki.platform.security.requiredrights.display.BlockSupplierProvider;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.objects.BaseObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SkinExtensionObjectRequiredRightAnalyzer}.
 *
 * @version $Id$
 */
@ComponentTest
class SkinExtensionObjectRequiredRightAnalyzerTest
{
    @MockComponent
    @Named("translation")
    private BlockSupplierProvider<String> translationMessageSupplierProvider;

    @MockComponent
    private BlockSupplierProvider<BaseObject> xObjectDisplayerProvider;

    @InjectMockComponents
    private SkinExtensionObjectRequiredRightAnalyzer analyzer;

    @ParameterizedTest
    @CsvSource(value = { "always, programming",
        "currentPage, script",
        "onDemand, script" })
    void analyze(String use, Right requiredRight) throws RequiredRightsException
    {
        BaseObject object = mock();
        when(object.getStringValue("use")).thenReturn(use);
        ObjectReference objectReference = mock();
        doReturn(objectReference).when(object).getReference();

        List<RequiredRightAnalysisResult> analysisResults = this.analyzer.analyze(object);

        assertEquals(1, analysisResults.size());
        RequiredRightAnalysisResult analysisResult = analysisResults.get(0);
        assertEquals(objectReference, analysisResult.getEntityReference());
        if ("always".equals(use)) {
            verify(this.translationMessageSupplierProvider).get("security.requiredrights.object.skinExtension.always");
        } else {
            verify(this.translationMessageSupplierProvider).get("security.requiredrights.object.skinExtension");
        }

        verify(this.xObjectDisplayerProvider).get(object);
        assertEquals(1, analysisResult.getRequiredRights().size());
        RequiredRight requiredRightResult = analysisResult.getRequiredRights().get(0);
        assertEquals(requiredRight, requiredRightResult.getRight());
        assertEquals(EntityType.DOCUMENT, requiredRightResult.getEntityType());
        assertFalse(requiredRightResult.isManualReviewNeeded());
    }
}
