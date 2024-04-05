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
package org.xwiki.uiextension;

import java.io.StringWriter;
import java.util.concurrent.Callable;

import org.apache.commons.collections.MapUtils;
import org.apache.velocity.VelocityContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.logging.LoggerConfiguration;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.authorization.AuthorExecutor;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.uiextension.internal.WikiUIExtensionConstants;
import org.xwiki.uiextension.internal.WikiUIExtensionParameters;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.velocity.XWikiVelocityException;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link WikiUIExtensionParametersTest}.
 *
 * @version $Id$
 * @since 5.0M1
 */
@ComponentTest
class WikiUIExtensionParametersTest
{
    private static final DocumentReference AUTHOR_REFERENCE = new DocumentReference("xwiki", "XWiki", "XWikiAdmin");

    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("xwiki", "Space", "UIX");

    @Mock
    private VelocityEngine velocityEngine;

    @MockComponent
    private ModelContext modelContext;

    @MockComponent
    private Execution execution;

    @MockComponent
    private VelocityManager velocityManager;

    @MockComponent
    private LoggerConfiguration loggerConfiguration;

    @MockComponent
    private AuthorExecutor authorExecutor;

    @MockComponent
    private AuthorizationManager authorizationManager;

    @RegisterExtension
    private final LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @BeforeEach
    public void setUp() throws Exception
    {
        VelocityContext velocityContext = new VelocityContext();
        ExecutionContext executionContext = mock(ExecutionContext.class);

        when(this.execution.getContext()).thenReturn(executionContext);
        when(this.velocityManager.getVelocityContext()).thenReturn(velocityContext);
        when(this.velocityManager.getVelocityEngine()).thenReturn(this.velocityEngine);

        when(this.authorExecutor.call(any(), any(), any())).thenAnswer(invocation -> {
            Callable<?> callable = invocation.getArgument(0);
            return callable.call();
        });

        when(this.authorizationManager.hasAccess(Right.SCRIPT, AUTHOR_REFERENCE, DOCUMENT_REFERENCE)).thenReturn(true);

        when(this.modelContext.getCurrentEntityReference()).thenReturn(new WikiReference("xwiki"));
    }

    @Test
    void getParametersWithAnEmptyParametersProperty() throws Exception
    {
        BaseObject mockUIX = constructMockUIXObject("");
        WikiUIExtensionParameters parameters = new WikiUIExtensionParameters(mockUIX, this.componentManager);
        assertEquals(MapUtils.EMPTY_MAP, parameters.get());
    }

    @Test
    void getParametersWithAnEqualSignInAValue() throws Exception
    {
        when(this.velocityEngine
            .evaluate(any(VelocityContext.class), any(StringWriter.class), eq("id:key"), eq("value")))
            .thenReturn(true);
        BaseObject mockUIX = constructMockUIXObject("key=value");
        WikiUIExtensionParameters parameters = new WikiUIExtensionParameters(mockUIX, this.componentManager);

        // Since the StringWriter is created within the method, the value is "" and not "value".
        assertEquals("", parameters.get().get("key"));
    }

    @Test
    void getParametersWithCommentAloneOnLine() throws Exception
    {
        String paramsStr = "# a = 1\n"
            + "x=1\n"
            + "y=2\n"
            + "# ...\n"
            + "z=3";
        BaseObject mockUIX = constructMockUIXObject(paramsStr);
        WikiUIExtensionParameters parameters = new WikiUIExtensionParameters(mockUIX, this.componentManager);
        parameters.get();

        verify(this.velocityEngine).evaluate(any(), any(), eq("id:x"), eq("1"));
        verify(this.velocityEngine).evaluate(any(), any(), eq("id:y"), eq("2"));
        verify(this.velocityEngine).evaluate(any(), any(), eq("id:z"), eq("3"));
    }

    @Test
    void getParametersWithCommentEndOfLine() throws Exception
    {
        String paramsStr = "x=1##b\n"
            + "y=2####x\n"
            + "z=3 ## xyz\n";
        BaseObject mockUIX = constructMockUIXObject(paramsStr);
        WikiUIExtensionParameters parameters = new WikiUIExtensionParameters(mockUIX, this.componentManager);
        parameters.get();

        verify(this.velocityEngine).evaluate(any(), any(), eq("id:x"), eq("1##b"));
        verify(this.velocityEngine).evaluate(any(), any(), eq("id:y"), eq("2####x"));
        verify(this.velocityEngine).evaluate(any(), any(), eq("id:z"), eq("3 ## xyz"));
    }

    @Test
    void getParametersWhenVelocityFails() throws Exception
    {
        when(this.velocityEngine
            .evaluate(any(VelocityContext.class), any(StringWriter.class), eq("id:key"), eq("value")))
            .thenThrow(new XWikiVelocityException(""));
        BaseObject mockUIX = constructMockUIXObject("key=value");
        WikiUIExtensionParameters parameters = new WikiUIExtensionParameters(mockUIX, this.componentManager);

        // It put a warning in the logs and return the not-evaluated value.
        assertEquals("value", parameters.get().get("key"));
        assertEquals("Failed to evaluate UI extension data value, key [key], value [value]. Reason: []",
            this.logCapture.getMessage(0));
    }

    @Test
    void getParametersFromTheSameRequestAndForTheSameWiki() throws Exception
    {
        when(this.velocityEngine
            .evaluate(any(VelocityContext.class), any(StringWriter.class), eq("id:key"), eq("value")))
            .thenReturn(true);
        BaseObject mockUIX = constructMockUIXObject("key=value");
        WikiUIExtensionParameters parameters = new WikiUIExtensionParameters(mockUIX, this.componentManager);

        // It should fail silently
        assertEquals("", parameters.get().get("key"));
        assertEquals("", parameters.get().get("key"));

        // Verify the evaluate is done only once
        verify(this.velocityEngine)
            .evaluate(any(VelocityContext.class), any(StringWriter.class), eq("id:key"), eq("value"));
    }

    @Test
    void getParametersFromTheSameRequestButForDifferentWikis() throws Exception
    {
        when(this.modelContext.getCurrentEntityReference()).thenReturn(new WikiReference("wiki1"))
            .thenReturn(new WikiReference("wiki2"));
        when(this.velocityEngine
            .evaluate(any(VelocityContext.class), any(StringWriter.class), eq("id:key"), eq("value")))
            .thenReturn(true);
        BaseObject mockUIX = constructMockUIXObject("key=value");
        WikiUIExtensionParameters parameters = new WikiUIExtensionParameters(mockUIX, this.componentManager);

        // It should fail silently
        assertEquals("", parameters.get().get("key"));
        assertEquals("", parameters.get().get("key"));

        // Verify the velocity evaluation has been done for both wikis.
        verify(this.velocityEngine, times(2))
            .evaluate(any(VelocityContext.class), any(StringWriter.class), eq("id:key"), eq("value"));
    }

    @Test
    void getParametersFromDifferentRequests() throws Exception
    {
        when(this.velocityEngine
            .evaluate(any(VelocityContext.class), any(StringWriter.class), eq("id:key"), eq("value")))
            .thenReturn(true);
        BaseObject mockUIX = constructMockUIXObject("key=value");
        WikiUIExtensionParameters parameters = new WikiUIExtensionParameters(mockUIX, this.componentManager);

        ExecutionContext ec1 = mock(ExecutionContext.class, "ec1");
        ExecutionContext ec2 = mock(ExecutionContext.class, "ec2");
        when(this.execution.getContext()).thenReturn(ec1).thenReturn(ec2);

        // It should fail silently
        assertEquals("", parameters.get().get("key"));
        assertEquals("", parameters.get().get("key"));

        // Verify the velocity evaluation has been done for both wikis.
        verify(this.velocityEngine, times(2))
            .evaluate(any(VelocityContext.class), any(StringWriter.class), eq("id:key"), eq("value"));
    }

    @Test
    void getParametersWithoutScriptRight() throws Exception
    {
        when(this.authorizationManager.hasAccess(Right.SCRIPT, AUTHOR_REFERENCE, DOCUMENT_REFERENCE)).thenReturn(false);
        BaseObject mockUIX = constructMockUIXObject("key=value");
        WikiUIExtensionParameters parameters = new WikiUIExtensionParameters(mockUIX, this.componentManager);

        assertEquals("value", parameters.get().get("key"));
        verifyNoInteractions(this.velocityEngine);
    }

    @Test
    void getParametersWithAuthorExecutor() throws Exception
    {
        // Do not call the callable to check that the call to the Velocity engine is inside the author executor.
        doReturn(null).when(this.authorExecutor).call(any(), any(), any());

        BaseObject mockUIX = constructMockUIXObject("key=value");
        WikiUIExtensionParameters parameters = new WikiUIExtensionParameters(mockUIX, this.componentManager);

        assertEquals("value", parameters.get().get("key"));
        verify(this.authorExecutor).call(any(), eq(AUTHOR_REFERENCE), eq(DOCUMENT_REFERENCE));
        verifyNoInteractions(this.velocityEngine);
    }

    @Test
    void getParametersWithEmptyKey() throws Exception
    {
        BaseObject mockUIX = constructMockUIXObject("=value");
        WikiUIExtensionParameters parameters = new WikiUIExtensionParameters(mockUIX, this.componentManager);

        assertTrue(parameters.get().isEmpty());
    }

    private BaseObject constructMockUIXObject(String parameters)
    {
        BaseObject result = mock();

        when(result.getStringValue(WikiUIExtensionConstants.ID_PROPERTY)).thenReturn("id");
        when(result.getStringValue(WikiUIExtensionConstants.PARAMETERS_PROPERTY)).thenReturn(parameters);
        when(result.getOwnerDocument()).thenReturn(mock(XWikiDocument.class));
        when(result.getOwnerDocument().getAuthorReference()).thenReturn(WikiUIExtensionParametersTest.AUTHOR_REFERENCE);
        when(result.getDocumentReference()).thenReturn(WikiUIExtensionParametersTest.DOCUMENT_REFERENCE);

        return result;
    }
}
