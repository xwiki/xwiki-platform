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
package org.xwiki.localization.wiki.internal;

import java.util.List;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.platform.security.requiredrights.RequiredRight;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightsException;
import org.xwiki.platform.security.requiredrights.display.BlockSupplierProvider;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
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
    @Named("stringCode")
    private BlockSupplierProvider<String> stringCodeBlockSupplierProvider;

    @MockComponent
    private BlockSupplierProvider<BaseObject> xObjectDisplayerProvider;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private WikiTranslationConfiguration translationConfiguration;

    @Mock
    private XWikiContext context;

    @Mock
    private BaseObject object;

    @Mock
    private ObjectReference objectReference;

    private XWikiDocument document;

    private String documentContent;

    @BeforeEach
    void setup()
    {
        this.document = new XWikiDocument(new DocumentReference("xwiki", "Test", "Translation"));
        this.documentContent = "localization.test=translation";
        this.document.setContent(this.documentContent);
        when(this.contextProvider.get()).thenReturn(this.context);
        when(this.context.getDoc()).thenReturn(this.document);

        when(this.translationConfiguration.isRestrictUserTranslations()).thenReturn(true);

        doReturn(this.objectReference).when(this.object).getReference();
    }

    @ParameterizedTest
    @CsvSource(value = {"GLOBAL, programming", "WIKI, admin", "USER, script" })
    void analyze(String scope, Right requiredRight) throws RequiredRightsException
    {
        when(this.object.getStringValue("scope")).thenReturn(scope);

        List<RequiredRightAnalysisResult> analysisResults = this.analyzer.analyze(this.object);

        assertEquals(2, analysisResults.size());
        RequiredRightAnalysisResult analysisResult = analysisResults.get(0);
        assertEquals(this.objectReference, analysisResult.getEntityReference());
        verify(this.translationMessageSupplierProvider).get("localization.requiredrights.translationDocument."
            + scope.toLowerCase());
        verify(this.xObjectDisplayerProvider).get(this.object);

        assertEquals(1, analysisResult.getRequiredRights().size());
        RequiredRight requiredRightResult = analysisResult.getRequiredRights().get(0);
        assertEquals(requiredRight, requiredRightResult.getRight());
        if (scope.equals("WIKI")) {
            assertEquals(EntityType.WIKI, requiredRightResult.getEntityType());
        } else {
            assertEquals(EntityType.DOCUMENT, requiredRightResult.getEntityType());
        }
        assertFalse(requiredRightResult.isManualReviewNeeded());

        analysisResult = analysisResults.get(1);
        assertEquals(this.document.getDocumentReference(), analysisResult.getEntityReference());
        verify(this.translationMessageSupplierProvider).get("localization.requiredrights.translationDocument."
            + scope.toLowerCase() + ".content");
        verify(this.stringCodeBlockSupplierProvider).get(this.documentContent);

        assertEquals(1, analysisResult.getRequiredRights().size());
        requiredRightResult = analysisResult.getRequiredRights().get(0);
        assertEquals(requiredRight, requiredRightResult.getRight());
        if (scope.equals("WIKI")) {
            assertEquals(EntityType.WIKI, requiredRightResult.getEntityType());
        } else {
            assertEquals(EntityType.DOCUMENT, requiredRightResult.getEntityType());
        }
        assertFalse(requiredRightResult.isManualReviewNeeded());
    }
}
