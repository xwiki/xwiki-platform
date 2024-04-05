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
import java.util.Set;

import javax.inject.Named;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.platform.security.requiredrights.RequiredRight;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.internal.provider.BlockSupplierProvider;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.velocity.internal.util.VelocityDetector;

import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Component tests for {@link PDFClassObjectRequiredRightsAnalyzer}.
 *
 * @version $Id$
 */
@ComponentTest
@ComponentList({ VelocityDetector.class })
class PDFClassObjectRequiredRightsAnalyzerTest
{
    @MockComponent
    @Named("translation")
    private BlockSupplierProvider<String> translationMessageSupplierProvider;

    @MockComponent
    @Named("stringCode")
    private BlockSupplierProvider<String> stringCodeBlockSupplierProvider;

    @InjectMockComponents
    private PDFClassObjectRequiredRightsAnalyzer analyzer;

    @ParameterizedTest
    @CsvSource({ "$hello, true", "hello, false" })
    void analyze(String value, boolean isVelocity) throws Exception
    {
        BaseObject object = mock(BaseObject.class);
        String propertyName = "style";
        when(object.getPropertyList()).thenReturn(Set.of(propertyName));
        BaseProperty<EntityReference> styleProperty = mock();
        EntityReference stylePropertyReference = mock(EntityReference.class);
        when(styleProperty.getReference()).thenReturn(stylePropertyReference);
        when(styleProperty.getValue()).thenReturn(value);
        when(object.get(propertyName)).thenReturn(styleProperty);

        List<RequiredRightAnalysisResult> result = this.analyzer.analyze(object);
        if (isVelocity) {
            assertEquals(1, result.size());
            assertEquals(RequiredRight.SCRIPT_AND_MAYBE_PROGRAM, result.get(0).getRequiredRights());
            assertEquals(stylePropertyReference, result.get(0).getEntityReference());

            verify(this.stringCodeBlockSupplierProvider).get(value);
            verify(this.translationMessageSupplierProvider)
                .get("security.requiredrights.object.pdfClass", propertyName);
        } else {
            assertEquals(0, result.size());
        }
    }
}
