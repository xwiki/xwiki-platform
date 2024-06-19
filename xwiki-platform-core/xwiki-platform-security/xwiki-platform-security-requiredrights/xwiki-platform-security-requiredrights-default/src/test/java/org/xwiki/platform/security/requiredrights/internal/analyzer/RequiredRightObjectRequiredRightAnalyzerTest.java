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

import org.junit.jupiter.api.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.platform.security.requiredrights.RequiredRight;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.display.BlockSupplierProvider;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.objects.BaseObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RequiredRightObjectRequiredRightAnalyzer}.
 *
 * @version $Id$
 */
@ComponentTest
class RequiredRightObjectRequiredRightAnalyzerTest
{
    protected static final String LEVEL = "level";

    @MockComponent
    @Named("translation")
    private BlockSupplierProvider<String> translationMessageSupplierProvider;

    @InjectMockComponents
    private RequiredRightObjectRequiredRightAnalyzer analyzer;

    @Test
    void analyzeLegalRight() throws Exception
    {
        BaseObject object = mock();
        when(object.getStringValue(LEVEL)).thenReturn("programming");
        ObjectReference objectReference = mock();
        doReturn(objectReference).when(object).getReference();
        DocumentReference documentReference = mock();
        when(object.getDocumentReference()).thenReturn(documentReference);
        String viewDisplay = "Programming";
        when(object.displayView(eq(LEVEL), any())).thenReturn(viewDisplay);

        List<RequiredRightAnalysisResult> result = this.analyzer.analyze(object);

        assertEquals(2, result.size());

        verify(this.translationMessageSupplierProvider)
            .get("security.requiredrights.object.requiredRight", viewDisplay);
        verify(this.translationMessageSupplierProvider)
            .get("security.requiredrights.object.requiredRight.content", viewDisplay);
        for (RequiredRightAnalysisResult analysisResult : result) {
            assertEquals(1, analysisResult.getRequiredRights().size());
            RequiredRight requiredRight = analysisResult.getRequiredRights().get(0);
            assertEquals(Right.PROGRAM, requiredRight.getRight());
            assertEquals(EntityType.DOCUMENT, requiredRight.getEntityType());
            assertFalse(requiredRight.isManualReviewNeeded());
        }
        assertEquals(objectReference, result.get(0).getEntityReference());
        assertEquals(documentReference, result.get(1).getEntityReference());
    }

    @Test
    void analyzeIllegalRight() throws Exception
    {
        BaseObject object = mock(BaseObject.class);
        when(object.getStringValue(LEVEL)).thenReturn("xyz");

        List<RequiredRightAnalysisResult> result = this.analyzer.analyze(object);

        assertEquals(0, result.size());
    }

}
