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
package com.xpn.xwiki.internal.template;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.context.Context;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.environment.Environment;
import org.xwiki.rendering.internal.parser.MissingParserException;
import org.xwiki.rendering.internal.transformation.MutableRenderingContext;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.RenderingContext;
import org.xwiki.test.annotation.AfterComponent;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.velocity.XWikiVelocityException;

@AllComponents
public class WikiTemplateRendererTest
{
    @Rule
    public final MockitoComponentMockingRule<WikiTemplateRenderer> mocker =
        new MockitoComponentMockingRule<WikiTemplateRenderer>(WikiTemplateRenderer.class);

    private Environment environmentmMock;

    private VelocityManager velocityManagerMock;

    private VelocityEngine velocityEngineMock;

    @Before
    public void before() throws XWikiVelocityException
    {
        this.velocityEngineMock = mock(VelocityEngine.class);

        when(this.velocityManagerMock.getVelocityContext()).thenReturn(new VelocityContext());
        when(this.velocityManagerMock.getVelocityEngine()).thenReturn(this.velocityEngineMock);
    }

    @AfterComponent
    public void afterComponent() throws Exception
    {
        this.environmentmMock = this.mocker.registerMockComponent(Environment.class);
        this.velocityManagerMock = this.mocker.registerMockComponent(VelocityManager.class);
    }

    private void setTemplateContent(String content) throws UnsupportedEncodingException
    {
        when(this.environmentmMock.getResourceAsStream("/templates/template")).thenReturn(
            new ByteArrayInputStream(content.getBytes("UTF8")));
    }

    // Tests

    @Test
    public void testRender() throws Exception
    {
        when(
            this.velocityEngineMock.evaluate(Matchers.<Context> any(), Matchers.<Writer> any(), anyString(),
                eq("<html>$toto</html>"))).then(new Answer<Boolean>()
        {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable
            {
                Writer writer = (Writer) invocation.getArguments()[1];

                writer.write("<html>value</html>");

                return Boolean.TRUE;
            }
        });

        setTemplateContent("##raw.syntax=plain/1.0\n<html>$toto</html>");

        assertEquals("<html>value</html>", mocker.getComponentUnderTest().render("template"));
    }

    @Test
    public void testRenderWithoutRawSyntax() throws ComponentLookupException, ParseException, MissingParserException,
        IOException, XWikiVelocityException
    {
        when(
            this.velocityEngineMock.evaluate(Matchers.<Context> any(), Matchers.<Writer> any(), anyString(),
                eq("<html>$toto</html>"))).then(new Answer<Boolean>()
        {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable
            {
                Writer writer = (Writer) invocation.getArguments()[1];

                writer.write("<html>value</html>");

                return Boolean.TRUE;
            }
        });

        setTemplateContent("<html>$toto</html>");

        this.mocker.<Execution> getInstance(Execution.class).pushContext(new ExecutionContext());
        ((MutableRenderingContext) this.mocker.getInstance(RenderingContext.class)).push(null, null, null, null, false,
            Syntax.XHTML_1_0);

        assertEquals("<html>value</html>", mocker.getComponentUnderTest().render("template"));
    }
}
