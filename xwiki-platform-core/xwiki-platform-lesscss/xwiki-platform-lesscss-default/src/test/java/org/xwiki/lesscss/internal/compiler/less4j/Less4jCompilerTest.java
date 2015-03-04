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
package org.xwiki.lesscss.internal.compiler.less4j;

import java.io.FileInputStream;
import java.io.StringWriter;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.skin.Resource;
import org.xwiki.skin.Skin;
import org.xwiki.skin.SkinManager;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateContent;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.github.sommeri.less4j.Less4jException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 */
public class Less4jCompilerTest
{
    @Rule
    public MockitoComponentMockingRule<Less4jCompiler> mocker = new MockitoComponentMockingRule<>(Less4jCompiler.class);

    private TemplateManager templateManager;

    private SkinManager skinManager;
    
    private Skin skin;
    
    @Before
    public void setUp() throws Exception
    {
        templateManager = mocker.getInstance(TemplateManager.class);
        skinManager = mocker.getInstance(SkinManager.class);
        skin = mock(Skin.class);
    }
    
    @Test
    public void compile() throws Exception
    {
        // Mocks
        when(skinManager.getSkin("skin")).thenReturn(skin);
        
        // Is is actually more an integration test than a unit test
        
        // Import 1
        when(skin.getResource("less/style.less.vm")).thenReturn(mock(Resource.class));
        StringWriter import1source = new StringWriter();
        IOUtils.copy(new FileInputStream(getClass().getResource("/style.less.vm").getFile()), import1source);
        when(templateManager.renderFromSkin("less/style.less.vm", skin)).thenReturn(import1source.toString());
        
        
        // Import 2
        when(skin.getResource("less/subdir/import2.less")).thenReturn(mock(Resource.class));
        Template import2 = mock(Template.class);
        when(templateManager.getTemplate("less/subdir/import2.less", skin)).thenReturn(import2);
        TemplateContent importContent2 = mock(TemplateContent.class);
        when(import2.getContent()).thenReturn(importContent2);
        StringWriter import2source = new StringWriter();
        IOUtils.copy(new FileInputStream(getClass().getResource("/import2.less").getFile()), import2source);
        when(importContent2.getContent()).thenReturn(import2source.toString());
        
        // Import 3
        when(skin.getResource("less/subdir/import3.less")).thenReturn(mock(Resource.class));
        Template import3 = mock(Template.class);
        when(templateManager.getTemplate("less/subdir/import3.less", skin)).thenReturn(import3);
        TemplateContent importContent3 = mock(TemplateContent.class);
        when(import3.getContent()).thenReturn(importContent3);
        StringWriter import3source = new StringWriter();
        IOUtils.copy(new FileInputStream(getClass().getResource("/import3.less").getFile()), import3source);
        when(importContent3.getContent()).thenReturn(import3source.toString());
        

        // Test
        StringWriter source = new StringWriter();
        IOUtils.copy(new FileInputStream(getClass().getResource("/style3.less").getFile()), source);
        String result = mocker.getComponentUnderTest().compile(source.toString(), "skin");
        
        // Verify
        StringWriter expected = new StringWriter();
        IOUtils.copy(new FileInputStream(getClass().getResource("/style3.css").getFile()), expected);
        assertEquals(expected.toString(), result);
    }

    @Test
    public void compileWhenImportDoesNotExist() throws Exception
    {
        // Mocks
        when(skinManager.getSkin("skin")).thenReturn(skin);

        // Is is actually more an integration test than a unit test

        // Test
        Less4jException caughtException = null;
        try {
            StringWriter source = new StringWriter();
            IOUtils.copy(new FileInputStream(getClass().getResource("/style3.less").getFile()), source);
            mocker.getComponentUnderTest().compile(source.toString(), "skin");
        } catch (Less4jException e) {
            caughtException = e;
        }

        // Verify
        assertNotNull(caughtException);
        StringWriter exceptionMessage = new StringWriter();
        IOUtils.copy(new FileInputStream(getClass().getResource("/lessException.txt").getFile()), exceptionMessage);
        assertEquals(exceptionMessage.toString(), caughtException.getMessage());
    }
}
