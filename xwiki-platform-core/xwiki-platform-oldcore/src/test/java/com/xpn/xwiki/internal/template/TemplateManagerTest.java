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
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.context.Context;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.configuration.internal.MemoryConfigurationSource;
import org.xwiki.environment.Environment;
import org.xwiki.rendering.transformation.TransformationManager;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.annotation.AfterComponent;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.internal.MockConfigurationSource;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.VelocityManager;

/**
 * Validate {@link DefaultTemplateManager}.
 * 
 * @version $Id$
 */
@AllComponents
public class TemplateManagerTest
{
    @Rule
    public final MockitoComponentMockingRule<TemplateManager> mocker =
        new MockitoComponentMockingRule<TemplateManager>(DefaultTemplateManager.class);

    private Environment environmentMock;

    private VelocityManager velocityManagerMock;

    private VelocityEngine velocityEngineMock;

    @Before
    public void before() throws Exception
    {
        this.velocityEngineMock = mock(VelocityEngine.class);

        when(this.velocityManagerMock.getVelocityContext()).thenReturn(new VelocityContext());
        when(this.velocityManagerMock.getVelocityEngine()).thenReturn(this.velocityEngineMock);

        MemoryConfigurationSource configuration = this.mocker.registerMemoryConfigurationSource();
        this.mocker.registerComponent(MockConfigurationSource.getDescriptor("all"), configuration);

        this.mocker.registerMockComponent(ContextualAuthorizationManager.class);
    }

    @AfterComponent
    public void afterComponent() throws Exception
    {
        this.environmentMock = this.mocker.registerMockComponent(Environment.class);
        this.velocityManagerMock = this.mocker.registerMockComponent(VelocityManager.class);
        this.mocker.registerMockComponent(ConfigurationSource.class);
        this.mocker.registerMockComponent(TransformationManager.class);
    }

    private void setTemplateContent(String content) throws UnsupportedEncodingException, MalformedURLException
    {
        when(this.environmentMock.getResourceAsStream("/templates/template")).thenReturn(
            new ByteArrayInputStream(content.getBytes("UTF8")));
        when(this.environmentMock.getResource("/templates/template")).thenReturn(new URL("http://url"));
    }

    // Tests

    @Test
    public void testRenderVelocity() throws Exception
    {
        when(
            this.velocityEngineMock.evaluate(Matchers.<Context>any(), Matchers.<Writer>any(), anyString(),
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

        assertEquals("<html>value</html>", mocker.getComponentUnderTest().render("template"));
    }

    @Test
    public void testRenderWiki() throws Exception
    {
        setTemplateContent("##!source.syntax=xwiki/2.1\nfirst line\\\\second line");

        assertEquals("first line\nsecond line", mocker.getComponentUnderTest().render("template"));
    }
}
