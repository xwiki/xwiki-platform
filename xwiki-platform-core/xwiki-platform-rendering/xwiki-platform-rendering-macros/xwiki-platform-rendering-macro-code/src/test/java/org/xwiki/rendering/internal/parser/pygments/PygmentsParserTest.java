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
package org.xwiki.rendering.internal.parser.pygments;

import java.io.StringReader;

import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.SimpleScriptContext;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.python.core.Py;
import org.xwiki.rendering.parser.HighlightParser;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PygmentsParser}, about the cost of highlighting rather than its result, which the rendering
 * tests of the code macro cover.
 *
 * @version $Id$
 */
@ComponentTest
@AllComponents(excludes = DefaultPygmentsParserConfiguration.class)
class PygmentsParserTest
{
    private static final String LANGUAGE = "java";

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @MockComponent
    private PygmentsParserConfiguration configuration;

    private HighlightParser parser;

    @BeforeEach
    void beforeEach() throws Exception
    {
        when(this.configuration.getStyle()).thenReturn("default");

        this.parser = this.componentManager.getInstance(HighlightParser.class, "default");
    }

    @Test
    void highlightDoesNotCompileScripts() throws Exception
    {
        // Load the lexer and everything else the highlighting needs.
        highlight(0);

        // Jython numbers the Java classes it compiles Python code into, compiling being much more expensive than
        // highlighting a snippet.
        int compiledScripts = getCompiledScriptCount();
        for (int i = 1; i <= 10; i++) {
            highlight(i);
        }

        assertEquals(compiledScripts, getCompiledScriptCount());
    }

    @Test
    void highlightUsesImmutableTokenDefinitions() throws Exception
    {
        highlight(0);

        // Iterating over a Python list synchronizes on it in Jython, which makes the threads highlighting code at the
        // same time block each other, since all the lexers of the same class share their token definitions. Check them
        // in the engine of the parser, since each engine has its own Python modules.
        CompiledScript script = (CompiledScript) FieldUtils.readField(this.parser, "script", true);
        ScriptContext scriptContext = new SimpleScriptContext();
        scriptContext.setAttribute("language", LANGUAGE, ScriptContext.ENGINE_SCOPE);
        script.getEngine().eval("""
            from pygments.lexers import get_lexer_by_name
            states = type(get_lexer_by_name(language))._tokens.values()
            immutable = len(states) > 0 and all(type(rules) is tuple for rules in states)
            """, scriptContext);

        assertTrue((Boolean) scriptContext.getAttribute("immutable"));
    }

    private int getCompiledScriptCount() throws IllegalAccessException
    {
        return (int) FieldUtils.readStaticField(Py.class, "nameindex", true);
    }

    private void highlight(int variant) throws Exception
    {
        String code = "public class Sample%d { int value = %d; }".formatted(variant, variant);
        assertFalse(this.parser.highlight(LANGUAGE, new StringReader(code)).isEmpty());
    }
}
