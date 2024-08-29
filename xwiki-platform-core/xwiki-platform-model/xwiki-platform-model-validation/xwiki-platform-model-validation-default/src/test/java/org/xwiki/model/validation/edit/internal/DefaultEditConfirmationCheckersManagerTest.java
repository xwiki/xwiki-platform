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
package org.xwiki.model.validation.edit.internal;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import javax.inject.Named;
import javax.inject.Provider;
import javax.servlet.http.HttpSession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.validation.edit.EditConfirmationChecker;
import org.xwiki.model.validation.edit.EditConfirmationCheckerResult;
import org.xwiki.model.validation.edit.EditConfirmationCheckerResults;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.XWikiRequest;

import static ch.qos.logback.classic.Level.WARN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of {@link DefaultEditConfirmationCheckersManager}.
 *
 * @version $Id$
 * @since 15.10RC1
 */
@ComponentTest
class DefaultEditConfirmationCheckersManagerTest
{
    private static final EditConfirmationCheckerResult WARNING_MESSAGE_1 =
        new EditConfirmationCheckerResult(new WordBlock("warning"), false, true);

    private static final EditConfirmationCheckerResult WARNING_MESSAGE_2 =
        new EditConfirmationCheckerResult(new WordBlock("warning 2"), false);

    private static final EditConfirmationCheckerResult ERROR_MESSAGE_1 =
        new EditConfirmationCheckerResult(new WordBlock("error"), true, true);

    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("xwiki", "Space", "Page");

    @InjectMockComponents
    private DefaultEditConfirmationCheckersManager manager;

    @MockComponent
    @Named("context")
    private ComponentManager componentManager;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @Mock
    private EditConfirmationChecker editConfirmationCheckerWarn;

    @Mock
    private EditConfirmationChecker editConfirmationCheckerWarn2;

    @Mock
    private EditConfirmationChecker editConfirmationCheckerError;

    @Mock
    private XWikiContext context;

    @Mock
    private XWikiDocument xWikiDocument;

    @Mock
    private XWikiRequest request;

    @Mock
    private HttpSession session;

    @BeforeEach
    void setUp()
    {
        when(this.editConfirmationCheckerWarn.check()).thenReturn(Optional.of(WARNING_MESSAGE_1));
        when(this.editConfirmationCheckerWarn2.check()).thenReturn(Optional.of(WARNING_MESSAGE_2));
        when(this.editConfirmationCheckerError.check()).thenReturn(Optional.of(ERROR_MESSAGE_1));
        when(this.xcontextProvider.get()).thenReturn(this.context);
        when(this.context.getDoc()).thenReturn(this.xWikiDocument);
        when(this.xWikiDocument.getDocumentReference()).thenReturn(DOCUMENT_REFERENCE);
        when(this.entityReferenceSerializer.serialize(DOCUMENT_REFERENCE)).thenReturn("xwiki:Page.Space");
        when(this.context.getRequest()).thenReturn(this.request);
        when(this.request.getSession()).thenReturn(this.session);
    }

    @Test
    void checkWithComponentManagerIssue() throws Exception
    {
        when(this.componentManager.<EditConfirmationChecker>getInstanceMap(EditConfirmationChecker.class))
            .thenThrow(ComponentLookupException.class);
        assertEquals(new EditConfirmationCheckerResults(), this.manager.check());
        assertEquals("Failed to resolve the map of [org.xwiki.model.validation.edit.EditConfirmationChecker]. "
            + "Cause: [ComponentLookupException: ]", this.logCapture.getMessage(0));
        assertEquals(WARN, this.logCapture.getLogEvent(0).getLevel());
    }

    @Test
    void check() throws Exception
    {
        // TreeMap is used to make the order in which the components are evaluated deterministic, making it easier
        // to assert the results.
        Map<String, EditConfirmationChecker> componentsMap = new TreeMap<>();
        componentsMap.put("warn", this.editConfirmationCheckerWarn);
        componentsMap.put("warn2", this.editConfirmationCheckerWarn2);
        when(this.componentManager.<EditConfirmationChecker>getInstanceMap(EditConfirmationChecker.class))
            .thenReturn(componentsMap);
        EditConfirmationCheckerResults expected = new EditConfirmationCheckerResults();
        expected.append(WARNING_MESSAGE_1);
        expected.append(WARNING_MESSAGE_2);
        assertEquals(expected, this.manager.check());
    }

    @Test
    void checkSkipForcedResult() throws Exception
    {
        when(this.componentManager.<EditConfirmationChecker>getInstanceMap(EditConfirmationChecker.class)).thenReturn(
            Map.of(
                "warn", this.editConfirmationCheckerWarn
            ));
        when(this.session.getAttribute("force_edit_xwiki:Page.Space_forced_warn")).thenReturn(true);

        assertEquals(new EditConfirmationCheckerResults(), this.manager.check());
    }

    @Test
    void checkErrorNotSkipped() throws Exception
    {
        when(this.componentManager.<EditConfirmationChecker>getInstanceMap(EditConfirmationChecker.class)).thenReturn(
            Map.of(
                "error", this.editConfirmationCheckerError
            ));
        when(this.session.getAttribute("force_edit_xwiki:Page.Space_forced_error")).thenReturn(true);

        EditConfirmationCheckerResults expected = new EditConfirmationCheckerResults();
        expected.append(ERROR_MESSAGE_1);
        assertEquals(expected, this.manager.check());
    }

    @Test
    void checkResultsEmpty()
    {
        EditConfirmationCheckerResults editConfirmationCheckerResults = new EditConfirmationCheckerResults();
        assertFalse(editConfirmationCheckerResults.isError());
        assertEquals(List.of(), editConfirmationCheckerResults.getErrorMessages());
        assertEquals(List.of(), editConfirmationCheckerResults.getWarningMessages());
    }

    @Test
    void force() throws Exception
    {
        // TreeMap is used to make the order in which the components are evaluated deterministic, making it easier
        // to assert the results.
        Map<String, EditConfirmationChecker> componentsMap = new TreeMap<>();
        componentsMap.put("error", this.editConfirmationCheckerError);
        componentsMap.put("warn", this.editConfirmationCheckerWarn);
        componentsMap.put("warn2", this.editConfirmationCheckerWarn2);
        when(this.componentManager.<EditConfirmationChecker>getInstanceMap(EditConfirmationChecker.class))
            .thenReturn(componentsMap);
        this.manager.force();
        verify(this.session).getAttribute("force_edit_xwiki:Page.Space_cached_error");
        verify(this.session).removeAttribute("force_edit_xwiki:Page.Space_cached_error");
        verify(this.session).setAttribute(eq("force_edit_xwiki:Page.Space_forced_error"), any());
        verify(this.session).getAttribute("force_edit_xwiki:Page.Space_cached_warn");
        verify(this.session).removeAttribute("force_edit_xwiki:Page.Space_cached_warn");
        verify(this.session).setAttribute(eq("force_edit_xwiki:Page.Space_forced_warn"), any());
        verify(this.session).getAttribute("force_edit_xwiki:Page.Space_cached_warn2");
        verify(this.session).removeAttribute("force_edit_xwiki:Page.Space_cached_warn2");
        verify(this.session).setAttribute(eq("force_edit_xwiki:Page.Space_forced_warn2"), any());
    }
}
