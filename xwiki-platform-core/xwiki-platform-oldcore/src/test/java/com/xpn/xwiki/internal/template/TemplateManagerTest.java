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
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.velocity.VelocityContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.configuration.internal.MemoryConfigurationSource;
import org.xwiki.environment.Environment;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.rendering.RenderingException;
import org.xwiki.rendering.transformation.TransformationManager;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.DocumentAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.template.Template;
import org.xwiki.test.annotation.AfterComponent;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.internal.MockConfigurationSource;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.test.mockito.StringReaderMatcher;
import org.xwiki.url.URLConfiguration;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.velocity.VelocityTemplate;
import org.xwiki.velocity.XWikiVelocityException;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Validate {@link DefaultTemplateManager}.
 * 
 * @version $Id$
 */
@AllComponents
@ComponentTest
class TemplateManagerTest
{
    private DocumentAuthorizationManager authorizationMock;

    private Environment environmentMock;

    private VelocityManager velocityManagerMock;

    private VelocityEngine velocityEngineMock;

    @InjectMockComponents
    private DefaultTemplateManager templateManager;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @MockComponent
    private URLConfiguration urlConfiguration;

    @AfterComponent
    void afterComponent() throws Exception
    {
        this.componentManager.registerMockComponent(ConfigurationSource.class);
        this.componentManager.registerMockComponent(TransformationManager.class);
        this.componentManager.registerMockComponent(ObservationManager.class);
        this.componentManager.registerMockComponent(ContextualAuthorizationManager.class);
        this.componentManager.registerMockComponent(AuthorizationManager.class);
        this.componentManager.registerMockComponent(WikiDescriptorManager.class);

        this.authorizationMock = this.componentManager.registerMockComponent(DocumentAuthorizationManager.class);
        this.environmentMock = this.componentManager.registerMockComponent(Environment.class);
        this.velocityManagerMock = this.componentManager.registerMockComponent(VelocityManager.class);

        MemoryConfigurationSource configuration = this.componentManager.registerMemoryConfigurationSource();
        this.componentManager.registerComponent(MockConfigurationSource.getDescriptor("all"), configuration);
    }

    @BeforeEach
    void before() throws Exception
    {
        this.velocityEngineMock = mock();
        when(this.velocityManagerMock.getVelocityEngine()).thenReturn(this.velocityEngineMock);
        when(this.velocityManagerMock.getVelocityContext()).thenReturn(new VelocityContext());

        when(this.environmentMock.getResource("/templates/")).thenReturn(new URL("file://templates/"));
    }

    private void setTemplateContent(String content) throws UnsupportedEncodingException, MalformedURLException
    {
        when(this.environmentMock.getResourceAsStream("/templates/template"))
            .thenReturn(new ByteArrayInputStream(content.getBytes("UTF8")));
        when(this.environmentMock.getResource("/templates/template")).thenReturn(new URL("file://templates/template"));
    }

    private void mockVelocity(String source, String result) throws XWikiVelocityException
    {
        final VelocityTemplate template = mock();

        when(this.velocityManagerMock.compile(any(), argThat(new StringReaderMatcher(source))))
            .then(new Answer<VelocityTemplate>()
            {
                @Override
                public VelocityTemplate answer(InvocationOnMock invocation) throws Throwable
                {
                    return template;
                }
            });

        doAnswer(new Answer<Boolean>()
        {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable
            {
                Writer writer = (Writer) invocation.getArguments()[1];

                writer.write(result);

                return Boolean.TRUE;
            }
        }).when(this.velocityEngineMock).evaluate(any(), any(), any(), same(template));
    }

    @Test
    void renderVelocity() throws Exception
    {
        mockVelocity("source", "OK");

        setTemplateContent("source");

        assertEquals("OK", this.templateManager.render("template"));
    }

    @Test
    void templateWithoutScriptRight() throws Exception
    {
        DocumentReference author = new DocumentReference("wiki", "space", "user");

        doThrow(AccessDeniedException.class).when(this.authorizationMock).checkAccess(Right.SCRIPT,
            EntityType.DOCUMENT, author, null);

        Template template = this.templateManager.createStringTemplate("", author);

        assertThrows(RenderingException.class, () -> this.templateManager.render(template, new StringWriter()));
    }

    @Test
    void templateCheatingProtection() throws Exception
    {
        when(this.environmentMock.getResource("/templates/../secure[]")).thenReturn(new URL("file://secure[]"));
        when(this.environmentMock.getResourceAsStream("/templates/../template[]"))
            .thenReturn(new ByteArrayInputStream("source".getBytes("UTF8")));

        mockVelocity("source", "KO");

        assertEquals("", this.templateManager.render("../secure[]"));
    }

    @Test
    void renderWiki() throws Exception
    {
        setTemplateContent("##!source.syntax=xwiki/2.1\nfirst line\\\\second line");

        assertEquals("<p>first line<br/>second line</p>", this.templateManager.render("template"));
    }

    @Test
    void renderClassloaderTemplate() throws Exception
    {
        mockVelocity("classloader template content", "OK");

        assertEquals("OK", this.templateManager.render("classloader_template.vm"));
    }
}
