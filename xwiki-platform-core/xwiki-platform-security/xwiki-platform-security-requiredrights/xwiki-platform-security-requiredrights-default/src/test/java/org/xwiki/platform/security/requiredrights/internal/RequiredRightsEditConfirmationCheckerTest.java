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
package org.xwiki.platform.security.requiredrights.internal;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Named;
import javax.inject.Provider;
import javax.script.ScriptContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.xwiki.model.document.DocumentAuthors;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.validation.edit.EditConfirmationCheckerResult;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalyzer;
import org.xwiki.platform.security.requiredrights.RequiredRightsException;
import org.xwiki.platform.security.requiredrights.internal.analyzer.XWikiDocumentRequiredRightAnalyzer;
import org.xwiki.platform.security.requiredrights.internal.configuration.RequiredRightsConfiguration;
import org.xwiki.platform.security.requiredrights.internal.configuration.RequiredRightsConfiguration.RequiredRightDocumentProtection;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

import static com.xpn.xwiki.doc.XWikiDocument.CKEY_TDOC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.xwiki.platform.security.requiredrights.internal.configuration.RequiredRightsConfiguration.RequiredRightDocumentProtection.DENY;
import static org.xwiki.platform.security.requiredrights.internal.configuration.RequiredRightsConfiguration.RequiredRightDocumentProtection.NONE;
import static org.xwiki.platform.security.requiredrights.internal.configuration.RequiredRightsConfiguration.RequiredRightDocumentProtection.WARNING;

/**
 * Test of {@link RequiredRightsEditConfirmationChecker}.
 *
 * @version $Id$
 * @since 15.9RC1
 */
@ComponentTest
class RequiredRightsEditConfirmationCheckerTest
{
    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("xwiki", "Space", "Page");

    private static final XDOM XDOM = new XDOM(List.of());

    @InjectMockComponents
    private RequiredRightsEditConfirmationChecker editConfirmationChecker;

    @MockComponent
    @Named(XWikiDocumentRequiredRightAnalyzer.ID)
    private RequiredRightAnalyzer<XWikiDocument> analyzer;

    @MockComponent
    private RequiredRightsChangedFilter requiredRightsChangedFilter;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    private TemplateManager templateManager;

    @MockComponent
    private ContextualAuthorizationManager authorization;

    @MockComponent
    private ScriptContextManager scriptContextManager;

    @MockComponent
    private RequiredRightsConfiguration requiredRightsConfiguration;

    @Mock
    private XWikiContext context;

    @Mock
    private XWikiDocument tdoc;

    @Mock
    private DocumentAuthors authors;

    @Mock
    private ScriptContext scriptContext;

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.xcontextProvider.get()).thenReturn(this.context);
        when(this.context.get(CKEY_TDOC)).thenReturn(this.tdoc);
        when(this.tdoc.getDocumentReference()).thenReturn(DOCUMENT_REFERENCE);
        when(this.tdoc.isNew()).thenReturn(false);
        when(this.tdoc.getAuthors()).thenReturn(this.authors);
        when(this.authorization.hasAccess(Right.EDIT, DOCUMENT_REFERENCE)).thenReturn(true);
        when(this.analyzer.analyze(this.tdoc)).thenReturn(List.of());
        when(this.requiredRightsChangedFilter.filter(this.authors, List.of()))
            .thenReturn(new RequiredRightsChangedResult());
        when(this.templateManager.executeNoException(anyString())).thenReturn(XDOM);
        when(this.scriptContextManager.getCurrentScriptContext()).thenReturn(this.scriptContext);
    }

    @Test
    void checkDocumentationProtectionNone()
    {
        when(this.requiredRightsConfiguration.getDocumentProtection()).thenReturn(NONE);
        assertEquals(Optional.empty(), this.editConfirmationChecker.check());
        verifyNoInteractions(this.xcontextProvider);
    }

    @Test
    void checkNoEditRight()
    {
        when(this.requiredRightsConfiguration.getDocumentProtection()).thenReturn(WARNING);
        when(this.authorization.hasAccess(Right.EDIT, DOCUMENT_REFERENCE)).thenReturn(false);
        assertEquals(Optional.empty(), this.editConfirmationChecker.check());
    }

    @Test
    void checkDocIsNew()
    {
        when(this.requiredRightsConfiguration.getDocumentProtection()).thenReturn(WARNING);
        when(this.tdoc.isNew()).thenReturn(true);
        assertEquals(Optional.empty(), this.editConfirmationChecker.check());
    }

    @Test
    void checkNoResults()
    {
        when(this.requiredRightsConfiguration.getDocumentProtection()).thenReturn(WARNING);
        assertEquals(Optional.empty(), this.editConfirmationChecker.check());
    }

    public static Stream<Arguments> checkAnalyzerErrorSource()
    {
        return Stream.of(
            Arguments.of(WARNING, false),
            Arguments.of(DENY, true)
        );
    }

    @ParameterizedTest
    @MethodSource("checkAnalyzerErrorSource")
    void checkAnalyzerError(RequiredRightDocumentProtection documentProtection, boolean isError) throws Exception
    {
        when(this.requiredRightsConfiguration.getDocumentProtection()).thenReturn(documentProtection);
        when(this.analyzer.analyze(this.tdoc)).thenThrow(new RequiredRightsException("error message", null));
        assertEquals(Optional.of(new EditConfirmationCheckerResult(XDOM, isError)),
            this.editConfirmationChecker.check());
        verify(this.templateManager)
            .executeNoException("security/requiredrights/requiredRightsEditConfirmationCheckerError.vm");
    }

    public static Stream<Arguments> checkSource()
    {
        return Stream.of(
            Arguments.of(WARNING, false),
            Arguments.of(DENY, true)
        );
    }

    @ParameterizedTest
    @MethodSource("checkSource")
    void check(RequiredRightDocumentProtection documentProtection, boolean isError) throws Exception
    {
        when(this.requiredRightsConfiguration.getDocumentProtection()).thenReturn(documentProtection);
        RequiredRightsChangedResult result = new RequiredRightsChangedResult();
        result.addToAdded(mock(RequiredRightAnalysisResult.class));
        when(this.requiredRightsChangedFilter.filter(any(), any())).thenReturn(result);
        assertEquals(Optional.of(new EditConfirmationCheckerResult(XDOM, isError)),
            this.editConfirmationChecker.check());
        verify(this.templateManager)
            .executeNoException("security/requiredrights/requiredRightsEditConfirmationChecker.vm");
    }
}
