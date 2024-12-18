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
import org.junit.jupiter.params.provider.ValueSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.platform.security.requiredrights.RequiredRight;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.internal.provider.BlockSupplierProvider;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.velocity.internal.util.VelocityDetector;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseObjectReference;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Component test for {@link ObjectPropertyRequiredRightAnalyzer}.
 *
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
class ObjectPropertyRequiredRightAnalyzerTest
{
    @InjectMockComponents
    private ObjectPropertyRequiredRightAnalyzer objectPropertyRequiredRightAnalyzer;

    @MockComponent
    private VelocityDetector velocityDetector;

    @MockComponent
    @Named("translation")
    private BlockSupplierProvider<String> translationMessageSupplierProvider;

    @MockComponent
    @Named("stringCode")
    private BlockSupplierProvider<String> stringCodeBlockSupplierProvider;

    @MockComponent
    private BlockSupplierProvider<BaseObject> objectBlockSupplierProvider;

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @Test
    void createObjectResult()
    {
        BaseObject object = mock();
        BaseObjectReference reference = mock();
        when(object.getReference()).thenReturn(reference);
        String translation = "translation";
        String parameter1 = "parameter1";
        String parameter2 = "parameter2";
        RequiredRightAnalysisResult analysisResult =
            this.objectPropertyRequiredRightAnalyzer.createObjectResult(object, RequiredRight.SCRIPT, translation,
                parameter1, parameter2);

        assertEquals(List.of(RequiredRight.SCRIPT), analysisResult.getRequiredRights());
        assertEquals(reference, analysisResult.getEntityReference());
        verify(this.translationMessageSupplierProvider).get(translation, parameter1, parameter2);
        verify(this.objectBlockSupplierProvider).get(object);
    }

    @Test
    void analyzeAllPropertiesAndAddObjectResult() throws Exception
    {
        DocumentReference classReference = new DocumentReference("wiki", "XWiki", "StandardClass");
        XWikiDocument classDocument = new XWikiDocument(classReference);
        BaseClass classObject = classDocument.getXClass();

        String velocityFieldName = "velocity";
        classObject.addTextAreaField(velocityFieldName, "Velocity", 80, 5, TextAreaClass.EditorType.TEXT.toString(),
            TextAreaClass.ContentType.VELOCITY_CODE.toString(), false);

        this.oldcore.getSpyXWiki().saveDocument(classDocument, this.oldcore.getXWikiContext());

        XWikiDocument testDocument = new XWikiDocument(new DocumentReference("wiki", "space", "page"));
        BaseObject testObject = testDocument.newXObject(classReference, this.oldcore.getXWikiContext());
        String velocityContent = "Velocity $velocity {{groovy}}{{/groovy}}";
        testObject.setLargeStringValue(velocityFieldName, velocityContent);

        when(this.velocityDetector.containsVelocityScript(velocityContent)).thenReturn(true);

        String adminMessage = "adminMessage";
        String parameter = "parameter";
        List<RequiredRightAnalysisResult> results =
            this.objectPropertyRequiredRightAnalyzer.analyzeAllPropertiesAndAddObjectResult(testObject,
                RequiredRight.WIKI_ADMIN, adminMessage, parameter);

        assertEquals(2, results.size());
        RequiredRightAnalysisResult result = results.get(0);
        assertEquals(RequiredRight.SCRIPT_AND_MAYBE_PROGRAM, result.getRequiredRights());
        assertEquals(new ObjectPropertyReference(velocityFieldName, testObject.getReference()),
            result.getEntityReference());
        verify(this.translationMessageSupplierProvider).get(adminMessage, parameter);
        verify(this.stringCodeBlockSupplierProvider).get(velocityContent);

        result = results.get(1);
        assertEquals(List.of(RequiredRight.WIKI_ADMIN), result.getRequiredRights());
        assertEquals(testObject.getReference(), result.getEntityReference());
        verify(this.objectBlockSupplierProvider).get(testObject);
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void analyzeVelocityScriptValue(boolean isScript)
    {
        String value = "velocityValue";
        when(this.velocityDetector.containsVelocityScript(value)).thenReturn(isScript);
        String translation = "velocityMessage";
        EntityReference reference = mock();

        List<RequiredRightAnalysisResult> results =
            this.objectPropertyRequiredRightAnalyzer.analyzeVelocityScriptValue(value, reference, translation);

        if (isScript) {
            assertEquals(1, results.size());
            RequiredRightAnalysisResult result = results.get(0);
            assertEquals(RequiredRight.SCRIPT_AND_MAYBE_PROGRAM, result.getRequiredRights());
            assertEquals(reference, result.getEntityReference());
            verify(this.translationMessageSupplierProvider).get(translation);
            verify(this.stringCodeBlockSupplierProvider).get(value);
        } else {
            assertEquals(0, results.size());
        }
    }
}
