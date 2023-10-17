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
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.uiextension.internal.WikiUIExtensionParameters;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.velocity.XWikiVelocityException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
    }

    @Test
    void getParametersWithAnEmptyParametersProperty() throws Exception
    {
        when(this.modelContext.getCurrentEntityReference()).thenReturn(new WikiReference("xwiki"));
        WikiUIExtensionParameters parameters = new WikiUIExtensionParameters("id", "", this.componentManager);
        assertEquals(MapUtils.EMPTY_MAP, parameters.get());
    }

    @Test
    void getParametersWithAnEqualSignInAValue() throws Exception
    {
        when(this.modelContext.getCurrentEntityReference()).thenReturn(new WikiReference("xwiki"));
        when(this.velocityEngine
            .evaluate(any(VelocityContext.class), any(StringWriter.class), eq("id:key"), eq("value")))
            .thenReturn(true);
        WikiUIExtensionParameters parameters = new WikiUIExtensionParameters("id", "key=value", this.componentManager);

        // Since the StringWriter is created within the method, the value is "" and not "value".
        assertEquals("", parameters.get().get("key"));
    }

    @Test
    void getParametersWithCommentAloneOnLine() throws Exception
    {
        when(this.modelContext.getCurrentEntityReference()).thenReturn(new WikiReference("xwiki"));

        String paramsStr = "# a = 1\n"
            + "x=1\n"
            + "y=2\n"
            + "# ...\n"
            + "z=3";
        WikiUIExtensionParameters parameters =
            new WikiUIExtensionParameters("id", paramsStr, this.componentManager);
        parameters.get();

        verify(this.velocityEngine).evaluate(any(), any(), eq("id:x"), eq("1"));
        verify(this.velocityEngine).evaluate(any(), any(), eq("id:y"), eq("2"));
        verify(this.velocityEngine).evaluate(any(), any(), eq("id:z"), eq("3"));
    }

    @Test
    void getParametersWithCommentEndOfLine() throws Exception
    {
        when(this.modelContext.getCurrentEntityReference()).thenReturn(new WikiReference("xwiki"));

        String paramsStr = "x=1##b\n"
            + "y=2####x\n"
            + "z=3 ## xyz\n";
        WikiUIExtensionParameters parameters =
            new WikiUIExtensionParameters("id", paramsStr, this.componentManager);
        parameters.get();

        verify(this.velocityEngine).evaluate(any(), any(), eq("id:x"), eq("1##b"));
        verify(this.velocityEngine).evaluate(any(), any(), eq("id:y"), eq("2####x"));
        verify(this.velocityEngine).evaluate(any(), any(), eq("id:z"), eq("3 ## xyz"));
    }

    @Test
    void getParametersWhenVelocityFails() throws Exception
    {
        when(this.modelContext.getCurrentEntityReference()).thenReturn(new WikiReference("xwiki"));
        when(this.velocityEngine
            .evaluate(any(VelocityContext.class), any(StringWriter.class), eq("id:key"), eq("value")))
            .thenThrow(new XWikiVelocityException(""));
        WikiUIExtensionParameters parameters = new WikiUIExtensionParameters("id", "key=value", this.componentManager);

        // It should fail and put a warn in the logs
        assertNull(parameters.get().get("key"));
        assertEquals("Failed to evaluate UI extension data value, key [key], value [value]. Reason: []",
            this.logCapture.getMessage(0));
    }

    @Test
    void getParametersFromTheSameRequestAndForTheSameWiki() throws Exception
    {
        when(this.modelContext.getCurrentEntityReference()).thenReturn(new WikiReference("xwiki"));
        when(this.velocityEngine
            .evaluate(any(VelocityContext.class), any(StringWriter.class), eq("id:key"), eq("value")))
            .thenReturn(true);
        WikiUIExtensionParameters parameters = new WikiUIExtensionParameters("id", "key=value", this.componentManager);

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
        WikiUIExtensionParameters parameters = new WikiUIExtensionParameters("id", "key=value", this.componentManager);

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
        when(this.modelContext.getCurrentEntityReference()).thenReturn(new WikiReference("wiki1"));
        when(this.velocityEngine
            .evaluate(any(VelocityContext.class), any(StringWriter.class), eq("id:key"), eq("value")))
            .thenReturn(true);
        WikiUIExtensionParameters parameters = new WikiUIExtensionParameters("id", "key=value", this.componentManager);

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
}
