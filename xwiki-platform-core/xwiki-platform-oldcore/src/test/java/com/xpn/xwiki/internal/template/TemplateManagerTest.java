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

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.velocity.VelocityContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.component.manager.ComponentLookupException;
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
import org.xwiki.test.mockito.StringReaderMatcher;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.velocity.XWikiVelocityException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.when;

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

    @Before
    public void before() throws Exception
    {
        when(this.velocityManagerMock.getVelocityContext()).thenReturn(new VelocityContext());

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
        when(this.environmentMock.getResourceAsStream("/templates/template"))
            .thenReturn(new ByteArrayInputStream(content.getBytes("UTF8")));
        when(this.environmentMock.getResource("/templates/template")).thenReturn(new URL("http://url"));
    }

    private void mockVelocity(String source, String result) throws XWikiVelocityException
    {
        when(this.velocityManagerMock.evaluate(Matchers.<Writer>any(), anyString(),
            argThat(new StringReaderMatcher(source)))).then(new Answer<Boolean>()
            {
                @Override
                public Boolean answer(InvocationOnMock invocation) throws Throwable
                {
                    Writer writer = (Writer) invocation.getArguments()[0];

                    writer.write(result);

                    return Boolean.TRUE;
                }
            });
    }

    // Tests

    @Test
    public void testRenderVelocity() throws Exception
    {
        mockVelocity("source", "OK");

        setTemplateContent("source");

        assertEquals("OK", mocker.getComponentUnderTest().render("template"));
    }

    @Test
    public void testRenderWiki() throws Exception
    {
        setTemplateContent("##!source.syntax=xwiki/2.1\nfirst line\\\\second line");

        assertEquals("<p>first line<br/>second line</p>", mocker.getComponentUnderTest().render("template"));
    }

    @Test
    public void testRenderClassloaderTemplate() throws ComponentLookupException, Exception
    {
        mockVelocity("classloader template content", "OK");

        assertEquals("OK", this.mocker.getComponentUnderTest().render("classloader_template.vm"));
    }
}
