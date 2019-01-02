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
package org.xwiki.rendering.script;

import java.io.StringReader;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.component.internal.ContextComponentManagerProvider;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroId;
import org.xwiki.rendering.macro.MacroIdFactory;
import org.xwiki.rendering.macro.MacroManager;
import org.xwiki.rendering.macro.descriptor.MacroDescriptor;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.test.mockito.StringReaderMatcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RenderingScriptService}.
 *
 * @version $Id$
 * @since 3.2M3
 */
@ComponentList({ContextComponentManagerProvider.class})
public class RenderingScriptServiceTest
{
    @Rule
    public MockitoComponentMockingRule<RenderingScriptService> mocker = new MockitoComponentMockingRule<>(
        RenderingScriptService.class);

    @Test
    public void parseAndRender() throws Exception
    {
        Parser parser = this.mocker.registerMockComponent(Parser.class, "plain/1.0");
        when(parser.parse(argThat(new StringReaderMatcher("some [[TODO]] stuff"))))
            .thenReturn(new XDOM(Collections.<Block>emptyList()));

        BlockRenderer blockRenderer = this.mocker.registerMockComponent(BlockRenderer.class, "xwiki/2.0");
        doAnswer(new Answer<Void>()
        {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                WikiPrinter printer = (WikiPrinter) invocationOnMock.getArguments()[1];
                printer.print("some ~[~[TODO]] stuff");
                return null;
            }
        }).when(blockRenderer).render(any(XDOM.class), any());

        XDOM xdom = this.mocker.getComponentUnderTest().parse("some [[TODO]] stuff", "plain/1.0");
        assertEquals("some ~[~[TODO]] stuff", this.mocker.getComponentUnderTest().render(xdom, "xwiki/2.0"));
    }

    @Test
    public void parseAndRenderWhenErrorInParse() throws Exception
    {
        Parser parser = this.mocker.registerMockComponent(Parser.class, "plain/1.0");
        when(parser.parse(new StringReader("some [[TODO]] stuff"))).thenThrow(new ParseException(("error")));

        Assert.assertNull(this.mocker.getComponentUnderTest().parse("some [[TODO]] stuff", "plain/1.0"));
    }

    @Test
    public void parseAndRenderWhenErrorInRender() throws Exception
    {
        Parser parser = this.mocker.registerMockComponent(Parser.class, "plain/1.0");
        when(parser.parse(new StringReader("some [[TODO]] stuff")))
            .thenReturn(new XDOM(Collections.<Block>emptyList()));

        XDOM xdom = this.mocker.getComponentUnderTest().parse("some [[TODO]] stuff", "plain/1.0");
        Assert.assertNull(this.mocker.getComponentUnderTest().render(xdom, "unknown"));
    }

    @Test
    public void resolveSyntax() throws Exception
    {
        assertEquals(Syntax.XWIKI_2_1, this.mocker.getComponentUnderTest().resolveSyntax("xwiki/2.1"));
    }

    @Test
    public void resolveSyntaxWhenInvalid() throws Exception
    {
        Assert.assertNull(this.mocker.getComponentUnderTest().resolveSyntax("unknown"));
    }

    @Test
    public void escape10Syntax() throws Exception
    {
        // Since the logic is pretty simple (prepend every character with an escape character), the below tests are
        // mostly for exemplification.
        // Note: Java escaped string "\\" == "\" (real string).
        assertEquals("\\\\", this.mocker.getComponentUnderTest().escape("\\", Syntax.XWIKI_1_0));
        assertEquals("\\*\\t\\e\\s\\t\\*", this.mocker.getComponentUnderTest()
            .escape("*test*", Syntax.XWIKI_1_0));
        assertEquals("\\a\\\\\\\\\\[\\l\\i\\n\\k\\>\\X\\.\\Y\\]",
            this.mocker.getComponentUnderTest().escape("a\\\\[link>X.Y]", Syntax.XWIKI_1_0));
        assertEquals("\\{\\p\\r\\e\\}\\v\\e\\r\\b\\a\\t\\i\\m\\{\\/\\p\\r\\e\\}", this.mocker
            .getComponentUnderTest().escape("{pre}verbatim{/pre}", Syntax.XWIKI_1_0));
        assertEquals("\\{\\m\\a\\c\\r\\o\\:\\s\\o\\m\\e\\=\\p\\a\\r\\a\\m\\e\\t\\e\\r\\}"
            + "\\c\\o\\n\\t\\e\\n\\t" + "\\{\\m\\a\\c\\r\\o\\}",
            this.mocker.getComponentUnderTest().escape("{macro:some=parameter}content{macro}", Syntax.XWIKI_1_0));
    }

    @Test
    public void escape20Syntax() throws Exception
    {
        // Since the logic is pretty simple (prepend every character with an escape character), the below tests are
        // mostly for exemplification.
        assertEquals("~~", this.mocker.getComponentUnderTest().escape("~", Syntax.XWIKI_2_0));
        assertEquals("~*~*~t~e~s~t~*~*", this.mocker.getComponentUnderTest()
            .escape("**test**", Syntax.XWIKI_2_0));
        // Note: Java escaped string "\\" == "\" (real string).
        assertEquals("~a~\\~\\~[~[~l~i~n~k~>~>~X~.~Y~]~]",
            this.mocker.getComponentUnderTest().escape("a\\\\[[link>>X.Y]]", Syntax.XWIKI_2_0));
        assertEquals("~{~{~{~v~e~r~b~a~t~i~m~}~}~}",
            this.mocker.getComponentUnderTest().escape("{{{verbatim}}}", Syntax.XWIKI_2_0));
        assertEquals(
                "~{~{~m~a~c~r~o~ ~s~o~m~e~=~'~p~a~r~a~m~e~t~e~r~'~}~}~c~o~n~t~e~n~t~{~{~/~m~a~c~r~o~}~}",
                this.mocker.getComponentUnderTest().escape("{{macro some='parameter'}}content{{/macro}}",
                    Syntax.XWIKI_2_0));
    }

    @Test
    public void escape21Syntax() throws Exception
    {
        // Since the logic is pretty simple (prepend every character with an escape character), the below tests are
        // mostly for exemplification.
        assertEquals("~~", this.mocker.getComponentUnderTest().escape("~", Syntax.XWIKI_2_1));
        assertEquals("~*~*~t~e~s~t~*~*", this.mocker.getComponentUnderTest()
            .escape("**test**", Syntax.XWIKI_2_1));
        // Note: Java escaped string "\\" == "\" (real string).
        assertEquals("~a~\\~\\~[~[~l~i~n~k~>~>~X~.~Y~]~]",
            this.mocker.getComponentUnderTest().escape("a\\\\[[link>>X.Y]]", Syntax.XWIKI_2_1));
        assertEquals("~{~{~{~v~e~r~b~a~t~i~m~}~}~}",
            this.mocker.getComponentUnderTest().escape("{{{verbatim}}}", Syntax.XWIKI_2_1));
        assertEquals(
                "~{~{~m~a~c~r~o~ ~s~o~m~e~=~'~p~a~r~a~m~e~t~e~r~'~}~}~c~o~n~t~e~n~t~{~{~/~m~a~c~r~o~}~}",
                this.mocker.getComponentUnderTest().escape("{{macro some='parameter'}}content{{/macro}}",
                    Syntax.XWIKI_2_1));
    }

    @Test
    public void escapeSpaces() throws Exception
    {
        assertEquals("\\a\\ \\*\\t\\e\\s\\t\\*",
            this.mocker.getComponentUnderTest().escape("a *test*", Syntax.XWIKI_1_0));
        assertEquals("~a~ ~*~*~t~e~s~t~*~*",
            this.mocker.getComponentUnderTest().escape("a **test**", Syntax.XWIKI_2_0));
        assertEquals("~a~ ~*~*~t~e~s~t~*~*",
            this.mocker.getComponentUnderTest().escape("a **test**", Syntax.XWIKI_2_1));
    }

    @Test
    public void escapeNewLines() throws Exception
    {
        assertEquals("\\a\\\n\\b", this.mocker.getComponentUnderTest().escape("a\nb", Syntax.XWIKI_1_0));
        assertEquals("~a~\n~b", this.mocker.getComponentUnderTest().escape("a\nb", Syntax.XWIKI_2_0));
        assertEquals("~a~\n~b", this.mocker.getComponentUnderTest().escape("a\nb", Syntax.XWIKI_2_1));
    }

    @Test
    public void escapeWithNullInput() throws Exception
    {
        Assert.assertNull("Unexpected non-null output for null input",
            this.mocker.getComponentUnderTest().escape(null, Syntax.XWIKI_2_1));
    }

    @Test
    public void escapeWithEmptyInput() throws Exception
    {
        assertEquals("", this.mocker.getComponentUnderTest().escape("", Syntax.XWIKI_2_1));
    }

    @Test
    public void escapeWithNullSyntax() throws Exception
    {
        Assert.assertNull("Unexpected non-null output for null syntax",
            this.mocker.getComponentUnderTest().escape("anything", null));
    }

    @Test
    public void escapeWithNullInputAndSyntax() throws Exception
    {
        Assert.assertNull("Unexpected non-null output for null input and syntax", this.mocker.getComponentUnderTest()
            .escape(null, null));
    }

    @Test
    public void escapeWithUnsupportedSyntax() throws Exception
    {
        Assert.assertNull("Unexpected non-null output for unsupported syntax", this.mocker.getComponentUnderTest()
            .escape("unsupported", Syntax.XHTML_1_0));
    }

    @Test
    public void getMacroDescriptors() throws Exception
    {
        MacroManager macroManager = this.mocker.registerMockComponent(MacroManager.class);
        MacroId macroId = new MacroId("macroid");
        when(macroManager.getMacroIds(Syntax.XWIKI_2_1)).thenReturn(Collections.singleton(macroId));
        Macro macro = mock(Macro.class);
        when(macroManager.getMacro(macroId)).thenReturn(macro);
        MacroDescriptor descriptor = mock(MacroDescriptor.class);
        when(macro.getDescriptor()).thenReturn(descriptor);

        List<MacroDescriptor> descriptors = this.mocker.getComponentUnderTest().getMacroDescriptors(Syntax.XWIKI_2_1);
        assertEquals(1, descriptors.size());
        assertSame(descriptor, descriptors.get(0));
    }

    @Test
    public void resolveMacroId() throws Exception
    {
        MacroId macroId = new MacroId("info", Syntax.XWIKI_2_1);
        MacroIdFactory macroIdFactory = this.mocker.registerMockComponent(MacroIdFactory.class);
        when(macroIdFactory.createMacroId(macroId.toString())).thenReturn(macroId);

        assertSame(macroId, this.mocker.getComponentUnderTest().resolveMacroId(macroId.toString()));
    }

    @Test
    public void resolveMacroIdInvalid() throws Exception
    {
        MacroIdFactory macroIdFactory = this.mocker.registerMockComponent(MacroIdFactory.class);
        when(macroIdFactory.createMacroId("foo")).thenThrow(new ParseException("Invalid macro id"));

        assertNull(this.mocker.getComponentUnderTest().resolveMacroId("foo"));
    }

    @Test
    public void getMacroDescriptor() throws Exception
    {
        MacroId macroId = new MacroId("macroId");
        Macro macro = mock(Macro.class);
        MacroDescriptor macroDescriptor = mock(MacroDescriptor.class);

        MacroManager macroManager = this.mocker.registerMockComponent(MacroManager.class);
        when(macroManager.exists(macroId)).thenReturn(true);
        when(macroManager.getMacro(macroId)).thenReturn(macro);
        when(macro.getDescriptor()).thenReturn(macroDescriptor);

        assertSame(macroDescriptor, this.mocker.getComponentUnderTest().getMacroDescriptor(macroId));
    }

    @Test
    public void getMacroDescriptorNotFound() throws Exception
    {
        MacroId macroId = new MacroId("macroId");
        MacroManager macroManager = this.mocker.registerMockComponent(MacroManager.class);
        when(macroManager.exists(macroId)).thenReturn(false);

        assertNull(this.mocker.getComponentUnderTest().getMacroDescriptor(macroId));
    }
}
