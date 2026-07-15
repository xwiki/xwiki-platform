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
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.skin.Resource;
import org.xwiki.skin.Skin;
import org.xwiki.skin.SkinManager;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateContent;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.github.sommeri.less4j.Less4jException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 */
@ComponentTest
class Less4jCompilerTest
{
    @InjectMockComponents
    private Less4jCompiler less4jCompiler;

    @MockComponent
    private TemplateManager templateManager;

    @MockComponent
    private SkinManager skinManager;

    @Mock
    private Skin skin;

    @Test
    void compile() throws Exception
    {
        // Mocks
        when(this.skinManager.getSkin("skin")).thenReturn(this.skin);

        // Is is actually more an integration test than a unit test

        // Import 1
        when(this.skin.getResource("less/style.less.vm")).thenReturn(mock(Resource.class));
        StringWriter import1source = new StringWriter();
        IOUtils.copy(new FileInputStream(getClass().getResource("/style.less.vm").getFile()), import1source);
        when(this.templateManager.renderFromSkin("less/style.less.vm", this.skin)).thenReturn(import1source.toString());

        // Import 2
        when(this.skin.getResource("less/subdir/import2.less")).thenReturn(mock(Resource.class));
        Template import2 = mock(Template.class);
        when(this.templateManager.getTemplate("less/subdir/import2.less", this.skin)).thenReturn(import2);
        TemplateContent importContent2 = mock(TemplateContent.class);
        when(import2.getContent()).thenReturn(importContent2);
        StringWriter import2source = new StringWriter();
        IOUtils.copy(new FileInputStream(getClass().getResource("/import2.less").getFile()), import2source);
        when(importContent2.getContent()).thenReturn(import2source.toString());

        // Import 3
        when(this.skin.getResource("less/subdir/import3.less")).thenReturn(mock(Resource.class));
        Template import3 = mock(Template.class);
        when(this.templateManager.getTemplate("less/subdir/import3.less", this.skin)).thenReturn(import3);
        TemplateContent importContent3 = mock(TemplateContent.class);
        when(import3.getContent()).thenReturn(importContent3);
        StringWriter import3source = new StringWriter();
        IOUtils.copy(new FileInputStream(getClass().getResource("/import3.less").getFile()), import3source);
        when(importContent3.getContent()).thenReturn(import3source.toString());

        // Test
        StringWriter source = new StringWriter();
        IOUtils.copy(new FileInputStream(getClass().getResource("/style3.less").getFile()), source);
        String result = this.less4jCompiler.compile(source.toString(), "skin", false);

        // Now with sourcemaps.
        String result2 = this.less4jCompiler.compile(source.toString(), "skin", true);

        // Verify
        StringWriter expected = new StringWriter();
        IOUtils.copy(new FileInputStream(getClass().getResource("/style3.css").getFile()), expected);
        assertEquals(expected.toString(), result);

        assertTrue(result2.contains("/*# sourceMappingURL=data:application/json;base64,"));
    }

    @Test
    void compileWhenImportDoesNotExist() throws Exception
    {
        // Mocks
        when(this.skinManager.getSkin("skin")).thenReturn(this.skin);

        // It is actually more an integration test than a unit test

        // Test
        Less4jException caughtException = null;
        try {
            StringWriter source = new StringWriter();
            IOUtils.copy(new FileInputStream(getClass().getResource("/style3.less").getFile()), source);
            this.less4jCompiler.compile(source.toString(), "skin", false);
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
