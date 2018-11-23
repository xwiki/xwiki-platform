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

import javax.script.ScriptContext;

import org.junit.jupiter.api.Test;
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
import static org.mockito.Mockito.mock;
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
    public TemplateManager templateManager;

    @MockComponent
    public ScriptContextManager scriptContextManager;

    @InjectMockComponents
    public DefaultTemplateHTMLDisplayer defaultTemplateHTMLDisplayer;

    @Test
    public void defaultTemplateHTMLDisplayerTest() throws Exception
    {
        ScriptContext scriptContext = mock(ScriptContext.class);
        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        Template template = mock(Template.class);

        assertEquals("html_displayer/default.vm",
                this.defaultTemplateHTMLDisplayer.getTemplateName(documentReference, "view"));

        when(templateManager.getTemplate("html_displayer/view.vm")).thenReturn(template);
        assertEquals("html_displayer/view.vm",
                this.defaultTemplateHTMLDisplayer.getTemplateName(documentReference, "view"));

        when(templateManager.getTemplate("html_displayer/documentreference.vm")).thenReturn(template);
        assertEquals("html_displayer/documentreference.vm",
                this.defaultTemplateHTMLDisplayer.getTemplateName(documentReference, "view"));

        when(templateManager.getTemplate("html_displayer/documentreference/view.vm")).thenReturn(template);
        assertEquals("html_displayer/documentreference/view.vm",
                this.defaultTemplateHTMLDisplayer.getTemplateName(documentReference, "view"));

        when(scriptContextManager.getCurrentScriptContext()).thenReturn(scriptContext);
        when(templateManager.render(any())).thenReturn("displayer");

        assertEquals("displayer", this.defaultTemplateHTMLDisplayer.display(documentReference));

        when(templateManager.render(any())).thenThrow(new Exception());

        assertThrows(HTMLDisplayerException.class,
                () -> this.defaultTemplateHTMLDisplayer.display(documentReference));
    }
}
