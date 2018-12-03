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
package org.xwiki.rendering.display.html.internal;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.script.ScriptContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.displayer.HTMLDisplayerException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link org.xwiki.displayer.HTMLDisplayer}.
 *
 * @version $Id$
 */
@ComponentTest
public class HTMLDisplayerTest
{
    @MockComponent
    private TemplateManager templateManager;

    @MockComponent
    private ScriptContextManager scriptContextManager;

    @InjectMockComponents
    private DefaultTemplateHTMLDisplayer defaultTemplateHTMLDisplayer;

    private DocumentReference documentReference;

    @BeforeEach
    public void setup()
    {
        ScriptContext scriptContext = mock(ScriptContext.class);
        this.documentReference = new DocumentReference("wiki", "space", "page");

        when(this.scriptContextManager.getCurrentScriptContext()).thenReturn(scriptContext);
    }

    @Test
    public void getTemplateTest() throws Exception
    {
        this.defaultTemplateHTMLDisplayer.display(DocumentReference.class, documentReference);
        verify(this.templateManager).getTemplate("html_displayer/documentreference/view.vm");
        verify(this.templateManager).getTemplate("html_displayer/documentreference.vm");
        verify(this.templateManager).getTemplate("html_displayer/view.vm");
        verify(this.templateManager).getTemplate("html_displayer/default.vm");
    }

    @Test
    public void getTemplateWithNullTypeTest() throws Exception
    {
        this.defaultTemplateHTMLDisplayer.display(null, "test");
        verify(this.templateManager).getTemplate("html_displayer/string/view.vm");
        verify(this.templateManager).getTemplate("html_displayer/string.vm");
        verify(this.templateManager).getTemplate("html_displayer/view.vm");
        verify(this.templateManager).getTemplate("html_displayer/default.vm");
    }

    @Test
    public void getTemplateWithNullValueTest() throws Exception
    {
        this.defaultTemplateHTMLDisplayer.display((new ArrayList<String>()).getClass(), null);
        verify(this.templateManager).getTemplate("html_displayer/arraylist/view.vm");
        verify(this.templateManager).getTemplate("html_displayer/arraylist.vm");
        verify(this.templateManager).getTemplate("html_displayer/view.vm");
        verify(this.templateManager).getTemplate("html_displayer/default.vm");
    }

    @Test
    public void getTemplateWithNullValueAndSpecialTypeTest() throws Exception
    {
        this.defaultTemplateHTMLDisplayer.display(new DefaultParameterizedType(List.class, String.class), null);
        verify(this.templateManager).getTemplate("html_displayer/java.util.list.java.lang.string/view.vm");
        verify(this.templateManager).getTemplate("html_displayer/java.util.list.java.lang.string.vm");
        verify(this.templateManager).getTemplate("html_displayer/view.vm");
        verify(this.templateManager).getTemplate("html_displayer/default.vm");
    }

    @Test
    public void getTemplateWithNullTypeAndNullValueTest() throws Exception
    {
        this.defaultTemplateHTMLDisplayer.display(null, null);
        verify(this.templateManager).getTemplate("html_displayer/view.vm");
        verify(this.templateManager).getTemplate("html_displayer/default.vm");
    }

    @Test
    public void displayTest() throws Exception
    {
        Template template = mock(Template.class);

        when(this.templateManager.getTemplate(any())).thenReturn(template);
        doAnswer(i -> ((StringWriter) i.getArgument(1)).append("displayer"))
                .when(templateManager).render(any(Template.class), any());

        assertEquals("displayer",
                this.defaultTemplateHTMLDisplayer.display(DocumentReference.class, documentReference));
    }

    @Test
    public void displayWithExceptionTest() throws Exception
    {
        Template template = mock(Template.class);

        when(this.templateManager.getTemplate(any())).thenReturn(template);
        doThrow(new Exception()).when(this.templateManager).render(any(Template.class), any());

        Throwable exception = assertThrows(HTMLDisplayerException.class,
                () -> this.defaultTemplateHTMLDisplayer.display(DocumentReference.class, documentReference));
        assertEquals("Couldn't render the template", exception.getMessage());
    }
}
