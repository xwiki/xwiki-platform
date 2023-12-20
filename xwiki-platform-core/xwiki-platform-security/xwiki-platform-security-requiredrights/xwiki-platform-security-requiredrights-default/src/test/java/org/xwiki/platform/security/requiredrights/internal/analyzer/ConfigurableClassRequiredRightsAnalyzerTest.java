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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.platform.security.requiredrights.RequiredRight;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightsException;
import org.xwiki.platform.security.requiredrights.internal.provider.BlockSupplierProvider;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.velocity.internal.util.VelocityDetector;

import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.LargeStringProperty;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link ConfigurableClassRequiredRightsAnalyzer}.
 *
 * @version $Id$
 */
@ComponentTest
@ComponentList({ VelocityDetector.class })
class ConfigurableClassRequiredRightsAnalyzerTest
{
    private static final String CODE_TO_EXECUTE = "codeToExecute";

    private static final String HEADING = "heading";

    @MockComponent
    @Named("translation")
    private BlockSupplierProvider<String> translationBlockSupplierProvider;

    @MockComponent
    @Named("stringCode")
    private BlockSupplierProvider<String> stringCodeBlockSupplierProvider;

    @InjectMockComponents
    private ConfigurableClassRequiredRightsAnalyzer configurableClassRequiredRightsAnalyzer;

    @Mock
    private EntityReference codeFieldReference;

    @Mock
    private EntityReference headingFieldReference;

    @Test
    void analyzeCodeToExecute() throws RequiredRightsException
    {
        String code = "Hello $World";
        BaseObject object = createMockXObject("Heading 1", code);
        List<RequiredRightAnalysisResult> analysisResults =
            this.configurableClassRequiredRightsAnalyzer.analyze(object);
        assertEquals(1, analysisResults.size());
        RequiredRightAnalysisResult result = analysisResults.get(0);
        assertEquals(this.codeFieldReference, result.getEntityReference());
        assertEquals(RequiredRight.SCRIPT_AND_MAYBE_PROGRAM, result.getRequiredRights());
        verify(this.translationBlockSupplierProvider).get("security.requiredrights.object.velocityWikiTextArea");
        verify(this.stringCodeBlockSupplierProvider).get(code);
    }

    @ParameterizedTest
    @CsvSource({
        "Heading, false",
        "$Heading, true"
    })
    void analyzeHeading(String heading, boolean requiresScript) throws RequiredRightsException
    {
        BaseObject object = createMockXObject(heading, "");
        List<RequiredRightAnalysisResult> analysisResults =
            this.configurableClassRequiredRightsAnalyzer.analyze(object);
        if (requiresScript) {
            assertEquals(1, analysisResults.size());
            RequiredRightAnalysisResult result = analysisResults.get(0);
            assertEquals(this.headingFieldReference, result.getEntityReference());
            assertEquals(RequiredRight.SCRIPT_AND_MAYBE_PROGRAM, result.getRequiredRights());
            verify(this.translationBlockSupplierProvider)
                .get("security.requiredrights.object.configurableClassHeading");
            verify(this.stringCodeBlockSupplierProvider).get(heading);
        } else {
            assertEquals(List.of(), analysisResults);
        }
    }

    private BaseObject createMockXObject(String heading, String code)
    {
        BaseObject result = mock();
        BaseClass xClass = mock();
        when(result.getXClass(any())).thenReturn(xClass);
        TextAreaClass codeProperty = mock();
        when(xClass.getField(CODE_TO_EXECUTE)).thenReturn(codeProperty);
        when(codeProperty.getContentType()).thenReturn(TextAreaClass.ContentType.VELOCITYWIKI.toString());

        LargeStringProperty codeField = mock();
        when(codeField.getValue()).thenReturn(code);
        when(codeField.getReference()).thenReturn(this.codeFieldReference);
        when(result.getField(CODE_TO_EXECUTE)).thenReturn(codeField);

        when(result.getStringValue(HEADING)).thenReturn(heading);
        StringProperty headingField = mock();
        when(headingField.getReference()).thenReturn(this.headingFieldReference);
        when(headingField.getValue()).thenReturn(heading);
        when(result.getField(HEADING)).thenReturn(headingField);

        return result;
    }
}
