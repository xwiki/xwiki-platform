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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.StringWriter;

import org.apache.commons.collections.MapUtils;
import org.apache.velocity.VelocityContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.LogRule;
import org.xwiki.test.mockito.MockitoComponentManagerRule;
import org.xwiki.uiextension.internal.WikiUIExtensionParameters;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.velocity.XWikiVelocityException;

/**
 * Unit tests for {@link WikiUIExtensionParametersTest}.
 *
 * @version $Id$
 * @since 5.0M1
 */
public class WikiUIExtensionParametersTest
{
    private VelocityEngine velocityEngine;

    private VelocityContext velocityContext;

    private ModelContext modelContext;

    private Execution execution;

    @Rule
    public LogRule logRule = new LogRule() {{
        record(LogLevel.WARN);
        recordLoggingForType(WikiUIExtensionParameters.class);
     }};

    @Rule
    public MockitoComponentManagerRule componentManager = new MockitoComponentManagerRule();

    @Before
    public void setUp() throws Exception
    {
        VelocityManager velocityManager = componentManager.registerMockComponent(VelocityManager.class);
        execution = componentManager.registerMockComponent(Execution.class);
        modelContext = componentManager.registerMockComponent(ModelContext.class);
        velocityEngine = mock(VelocityEngine.class);
        velocityContext = new VelocityContext();
        ExecutionContext executionContext = mock(ExecutionContext.class);

        when(execution.getContext()).thenReturn(executionContext);
        when(velocityManager.getVelocityContext()).thenReturn(velocityContext);
        when(velocityManager.getVelocityEngine()).thenReturn(velocityEngine);
    }

    @Test
    public void getParametersWithAnEmptyParametersProperty() throws Exception
    {
        when(modelContext.getCurrentEntityReference()).thenReturn(new WikiReference("xwiki"));
        WikiUIExtensionParameters parameters = new WikiUIExtensionParameters("id", "", componentManager);
        Assert.assertEquals(MapUtils.EMPTY_MAP, parameters.get());
    }

    @Test
    public void getParametersWithAnEqualSignInAValue() throws Exception
    {
        when(modelContext.getCurrentEntityReference()).thenReturn(new WikiReference("xwiki"));
        when(velocityEngine.evaluate(any(VelocityContext.class), any(StringWriter.class), eq("id:key"), eq("value")))
            .thenReturn(true);
        WikiUIExtensionParameters parameters = new WikiUIExtensionParameters("id", "key=value", componentManager);

        // Since the StringWriter is created within the method, the value is "" and not "value".
        Assert.assertEquals("", parameters.get().get("key"));
    }

    @Test
    public void getParametersWhenVelocityFails() throws Exception
    {
        when(modelContext.getCurrentEntityReference()).thenReturn(new WikiReference("xwiki"));
        when(velocityEngine.evaluate(any(VelocityContext.class), any(StringWriter.class), eq("id:key"), eq("value")))
            .thenThrow(new XWikiVelocityException(""));
        WikiUIExtensionParameters parameters = new WikiUIExtensionParameters("id", "key=value", componentManager);

        // It should fail and put a warn in the logs
        Assert.assertEquals(null, parameters.get().get("key"));
        Assert.assertTrue(
            logRule.contains("Failed to evaluate UI extension data value, key [key], value [value]. Reason: []"));
    }

    @Test
    public void getParametersFromTheSameRequestAndForTheSameWiki() throws Exception
    {
        when(modelContext.getCurrentEntityReference()).thenReturn(new WikiReference("xwiki"));
        when(velocityEngine.evaluate(any(VelocityContext.class), any(StringWriter.class), eq("id:key"), eq("value")))
            .thenReturn(true);
        WikiUIExtensionParameters parameters = new WikiUIExtensionParameters("id", "key=value", componentManager);

        // It should fail silently
        Assert.assertEquals("", parameters.get().get("key"));
        Assert.assertEquals("", parameters.get().get("key"));

        // Verify the evaluate is done only once
        verify(velocityEngine).evaluate(any(VelocityContext.class), any(StringWriter.class), eq("id:key"), eq("value"));
    }

    @Test
    public void getParametersFromTheSameRequestButForDifferentWikis() throws Exception
    {
        when(modelContext.getCurrentEntityReference()).thenReturn(new WikiReference("wiki1"))
            .thenReturn(new WikiReference("wiki2"));
        when(velocityEngine.evaluate(any(VelocityContext.class), any(StringWriter.class), eq("id:key"), eq("value")))
            .thenReturn(true);
        WikiUIExtensionParameters parameters = new WikiUIExtensionParameters("id", "key=value", componentManager);

        // It should fail silently
        Assert.assertEquals("", parameters.get().get("key"));
        Assert.assertEquals("", parameters.get().get("key"));

        // Verify the velocity evaluation has been done for both wikis.
        verify(velocityEngine, times(2)).evaluate(any(VelocityContext.class), any(StringWriter.class), eq("id:key"), eq("value"));
    }

    @Test
    public void getParametersFromDifferentRequests() throws Exception
    {
        when(modelContext.getCurrentEntityReference()).thenReturn(new WikiReference("wiki1"));
        when(velocityEngine.evaluate(any(VelocityContext.class), any(StringWriter.class), eq("id:key"), eq("value")))
            .thenReturn(true);
        WikiUIExtensionParameters parameters = new WikiUIExtensionParameters("id", "key=value", componentManager);

        ExecutionContext ec1 = mock(ExecutionContext.class, "ec1");
        ExecutionContext ec2 = mock(ExecutionContext.class, "ec2");
        when(execution.getContext()).thenReturn(ec1).thenReturn(ec2);

        // It should fail silently
        Assert.assertEquals("", parameters.get().get("key"));
        Assert.assertEquals("", parameters.get().get("key"));

        // Verify the velocity evaluation has been done for both wikis.
        verify(velocityEngine, times(2)).evaluate(any(VelocityContext.class), any(StringWriter.class), eq("id:key"), eq("value"));
    }
}
