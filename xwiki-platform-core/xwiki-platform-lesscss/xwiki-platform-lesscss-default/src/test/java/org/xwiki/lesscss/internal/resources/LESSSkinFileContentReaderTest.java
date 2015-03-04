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
package org.xwiki.lesscss.internal.resources;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.lesscss.compiler.LESSCompilerException;
import org.xwiki.lesscss.resources.LESSResourceReference;
import org.xwiki.lesscss.resources.LESSSkinFileResourceReference;
import org.xwiki.skin.Skin;
import org.xwiki.skin.SkinManager;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateContent;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @since 6.4M2
 * @version $Id$
 */
public class LESSSkinFileContentReaderTest
{
    @Rule
    public MockitoComponentMockingRule<LESSSkinFileReader> mocker =
            new MockitoComponentMockingRule<>(LESSSkinFileReader.class);

    private TemplateManager templateManager;

    private SkinManager skinManager;
    
    private Skin skin;

    @Before
    public void setUp() throws Exception
    {
        templateManager = mocker.getInstance(TemplateManager.class);
        skinManager = mocker.getInstance(SkinManager.class);
        skin = mock(Skin.class);
        when(skinManager.getSkin("skin")).thenReturn(skin);
    }

    @Test
    public void getContent() throws Exception
    {
        // Mocks
        Template template = mock(Template.class);
        when(templateManager.getTemplate("less/style.less", skin)).thenReturn(template);
        TemplateContent templateContent = mock(TemplateContent.class);
        when(template.getContent()).thenReturn(templateContent);
        when(templateContent.getContent()).thenReturn("// My LESS file");

        // Test
        assertEquals("// My LESS file", mocker.getComponentUnderTest().getContent(
                new LESSSkinFileResourceReference("style.less"), "skin"));
    }

    @Test
    public void getContentWithUnsupportedResource() throws Exception
    {
        // Test
        LESSCompilerException caughtException = null;
        try {
            mocker.getComponentUnderTest().getContent(new LESSResourceReference(){}, "skin");
        } catch (LESSCompilerException e) {
            caughtException = e;
        }

        // Verify
        assertNotNull(caughtException);
        assertEquals("Invalid LESS resource type.", caughtException.getMessage());
    }

    @Test
    public void getContentWhenFileDoesNotExist() throws Exception
    {
        // Test
        LESSCompilerException caughtException = null;
        try {
            mocker.getComponentUnderTest().getContent(
                    new LESSSkinFileResourceReference("not-existing-file.less"), "skin");
        } catch (LESSCompilerException e) {
            caughtException = e;
        }

        // Verify
        assertNotNull(caughtException);
        assertEquals("The template [not-existing-file.less] does not exists.", caughtException.getMessage());
    }

    @Test
    public void getContentWhenException() throws Exception
    {
        // Mocks
        Template template = mock(Template.class);
        when(templateManager.getTemplate("less/file.less", skin)).thenReturn(template);
        Exception exception = new Exception("exception");
        when(template.getContent()).thenThrow(exception);

        // Test
        LESSCompilerException caughtException = null;
        try {
            mocker.getComponentUnderTest().getContent(
                    new LESSSkinFileResourceReference("file.less"), "skin");
        } catch (LESSCompilerException e) {
            caughtException = e;
        }

        // Verify
        assertNotNull(caughtException);
        assertEquals("Failed to get the content of the template [file.less].", caughtException.getMessage());
        assertEquals(exception, caughtException.getCause());
    }

}
