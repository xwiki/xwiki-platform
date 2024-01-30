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
import org.junit.Test;
import org.xwiki.lesscss.compiler.LESSCompilerException;
import org.xwiki.skin.Skin;
import org.xwiki.skin.SkinManager;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateContent;
import org.xwiki.template.TemplateManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link LESSSkinFileResourceReference}.
 *
 * @since 7.0RC1
 * @version $Id$
 */
public class LESSSkinFileResourceReferenceTest
{
    private TemplateManager templateManager;

    private SkinManager skinManager;

    private Skin skin;
    
    @Before
    public void setUp() throws Exception
    {
        templateManager = mock(TemplateManager.class);
        skinManager = mock(SkinManager.class);
        skin = mock(Skin.class);
        when(skinManager.getSkin("skin")).thenReturn(skin);
    }

    @Test
    public void getContent() throws Exception
    {
        LESSSkinFileResourceReference lessSkinFileResourceReference
                = new LESSSkinFileResourceReference("style.less", templateManager, skinManager);
        
        // Mocks
        Template template = mock(Template.class);
        when(templateManager.getTemplate("less/style.less", skin)).thenReturn(template);
        TemplateContent templateContent = mock(TemplateContent.class);
        when(template.getContent()).thenReturn(templateContent);
        when(templateContent.getContent()).thenReturn("// My LESS file");

        // Test
        assertEquals("// My LESS file", lessSkinFileResourceReference.getContent("skin"));
    }

    @Test
    public void getContentWhenFileDoesNotExist() throws Exception
    {
        LESSSkinFileResourceReference lessSkinFileResourceReference
                = new LESSSkinFileResourceReference("not-existing-file.less", templateManager, skinManager);
        
        // Test
        LESSCompilerException caughtException = null;
        try {
            lessSkinFileResourceReference.getContent("skin");
        } catch (LESSCompilerException e) {
            caughtException = e;
        }

        // Verify
        assertNotNull(caughtException);
        assertEquals("The template [not-existing-file.less] does not exist.", caughtException.getMessage());
    }

    @Test
    public void getContentWhenException() throws Exception
    {
        LESSSkinFileResourceReference lessSkinFileResourceReference
                = new LESSSkinFileResourceReference("file.less", templateManager, skinManager);
        
        // Mocks
        Template template = mock(Template.class);
        when(templateManager.getTemplate("less/file.less", skin)).thenReturn(template);
        Exception exception = new Exception("exception");
        when(template.getContent()).thenThrow(exception);

        // Test
        LESSCompilerException caughtException = null;
        try {
           lessSkinFileResourceReference.getContent("skin");
        } catch (LESSCompilerException e) {
            caughtException = e;
        }

        // Verify
        assertNotNull(caughtException);
        assertEquals("Failed to get the content of the template [file.less].", caughtException.getMessage());
        assertEquals(exception, caughtException.getCause());
    }

    @Test
    public void serialize() throws Exception
    {
        LESSSkinFileResourceReference lessSkinFileResourceReference
                = new LESSSkinFileResourceReference("file.less", templateManager, skinManager);
        
        // Test
        assertEquals("LessSkinFile[file.less]", lessSkinFileResourceReference.serialize());
    }
}
