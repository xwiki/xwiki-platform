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
package org.xwiki.wysiwyg.server.internal.plugin.macro;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import javax.inject.Provider;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.xwiki.gwt.wysiwyg.client.plugin.macro.MacroService;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroId;
import org.xwiki.rendering.macro.MacroManager;
import org.xwiki.rendering.macro.descriptor.ContentDescriptor;
import org.xwiki.rendering.macro.descriptor.MacroDescriptor;
import org.xwiki.rendering.macro.descriptor.ParameterDescriptor;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxFactory;
import org.xwiki.rendering.syntax.SyntaxType;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wysiwyg.server.plugin.macro.MacroDescriptorTranslator;

import com.xpn.xwiki.XWikiContext;

/**
 * Unit tests for {@link XWikiMacroService}.
 * 
 * @version $Id$
 */
public class XWikiMacroServiceTest
{
    /**
     * A component manager that automatically mocks all dependencies of {@link XWikiMacroService}.
     */
    @Rule
    public MockitoComponentMockingRule<MacroService> mocker = new MockitoComponentMockingRule<MacroService>(
        XWikiMacroService.class);

    /**
     * Unit test for {@link XWikiMacroService#getMacroDescriptor(String, String)}.
     */
    @Test
    public void getMacroDescriptor() throws Exception
    {
        String macroId = "foo";
        String syntaxId = "syntax/x.y";

        // Verify that the specified syntax is used.
        SyntaxFactory syntaxFactory = mocker.getInstance(SyntaxFactory.class);
        Syntax syntax = new Syntax(SyntaxType.PLAIN, "x.y");
        when(syntaxFactory.createSyntaxFromIdString(syntaxId)).thenReturn(syntax);

        // Mock the macro descriptor.
        ContentDescriptor contentDescriptor = mock(ContentDescriptor.class);
        when(contentDescriptor.getDescription()).thenReturn("content description");
        when(contentDescriptor.isMandatory()).thenReturn(true);

        ParameterDescriptor parameterDescriptor = mock(ParameterDescriptor.class);
        when(parameterDescriptor.getId()).thenReturn("bar");
        when(parameterDescriptor.getName()).thenReturn("The Bar");
        when(parameterDescriptor.getDescription()).thenReturn("bar description");
        when(parameterDescriptor.getDefaultValue()).thenReturn("default value");
        when(parameterDescriptor.getParameterType()).thenReturn(Integer.class);
        when(parameterDescriptor.isMandatory()).thenReturn(true);

        MacroId macroIdObject = new MacroId(macroId, syntax);
        MacroDescriptor descriptor = mock(MacroDescriptor.class);
        when(descriptor.getId()).thenReturn(macroIdObject);
        when(descriptor.getName()).thenReturn("The Foo Macro");
        when(descriptor.getDescription()).thenReturn("foo description");
        when(descriptor.getContentDescriptor()).thenReturn(contentDescriptor);
        when(descriptor.getParameterDescriptorMap()).thenReturn(Collections.singletonMap("bar", parameterDescriptor));

        Macro macro = mock(Macro.class);
        when(macro.getDescriptor()).thenReturn(descriptor);
        when(macro.supportsInlineMode()).thenReturn(true);

        // Verify that the specified macro is retrieved.
        MacroManager macroManager = mocker.getInstance(MacroManager.class);
        when(macroManager.getMacro(macroIdObject)).thenReturn(macro);

        // Verify the macro descriptor is translated.
        MacroDescriptorTranslator macroDescriptorTranslator = mocker.getInstance(MacroDescriptorTranslator.class);
        org.xwiki.gwt.wysiwyg.client.plugin.macro.MacroDescriptor translatedDescriptor =
            new org.xwiki.gwt.wysiwyg.client.plugin.macro.MacroDescriptor();
        when(macroDescriptorTranslator.translate(any(org.xwiki.gwt.wysiwyg.client.plugin.macro.MacroDescriptor.class)))
            .thenReturn(translatedDescriptor);

        XWikiContext xcontext = mock(XWikiContext.class);
        when(xcontext.getWikiId()).thenReturn("xwiki");

        Provider<XWikiContext> contextProvider = mocker.registerMockComponent(XWikiContext.TYPE_PROVIDER);
        when(contextProvider.get()).thenReturn(xcontext);

        Assert.assertSame(translatedDescriptor, mocker.getComponentUnderTest().getMacroDescriptor(macroId, syntaxId));

        ArgumentCaptor<org.xwiki.gwt.wysiwyg.client.plugin.macro.MacroDescriptor> argument =
            ArgumentCaptor.forClass(org.xwiki.gwt.wysiwyg.client.plugin.macro.MacroDescriptor.class);
        verify(macroDescriptorTranslator).translate(argument.capture());
        org.xwiki.gwt.wysiwyg.client.plugin.macro.MacroDescriptor result = argument.getValue();

        Assert.assertEquals(descriptor.getId().getId(), result.getId());
        Assert.assertEquals(descriptor.getName(), result.getName());
        Assert.assertEquals(descriptor.getDescription(), result.getDescription());
        Assert.assertEquals(macro.supportsInlineMode(), result.isSupportingInlineMode());

        Assert.assertEquals("content", result.getContentDescriptor().getId());
        Assert.assertEquals("Content", result.getContentDescriptor().getName());
        Assert.assertEquals(contentDescriptor.getDescription(), result.getContentDescriptor().getDescription());
        Assert.assertNull(result.getContentDescriptor().getDefaultValue());
        Assert.assertEquals(contentDescriptor.isMandatory(), result.getContentDescriptor().isMandatory());
        Assert.assertEquals("java.lang.StringBuffer", result.getContentDescriptor().getType().getName());

        Assert.assertEquals(1, result.getParameterDescriptorMap().size());
        org.xwiki.gwt.wysiwyg.client.plugin.macro.ParameterDescriptor actualParamDescriptor =
            result.getParameterDescriptorMap().get(parameterDescriptor.getId());
        Assert.assertEquals(parameterDescriptor.getId(), actualParamDescriptor.getId());
        Assert.assertEquals(parameterDescriptor.getName(), actualParamDescriptor.getName());
        Assert.assertEquals(parameterDescriptor.getDescription(), actualParamDescriptor.getDescription());
        Assert.assertEquals(parameterDescriptor.getDefaultValue(), actualParamDescriptor.getDefaultValue());
        Assert.assertEquals(parameterDescriptor.isMandatory(), actualParamDescriptor.isMandatory());
        Assert.assertEquals("java.lang.Integer", actualParamDescriptor.getType().getName());
    }
}
