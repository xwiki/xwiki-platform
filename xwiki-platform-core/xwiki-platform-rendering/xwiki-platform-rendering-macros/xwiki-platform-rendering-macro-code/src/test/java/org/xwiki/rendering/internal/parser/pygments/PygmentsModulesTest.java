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

import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Validate that the Python modules used by {@link PygmentsParser} are compiled at build time.
 *
 * @version $Id$
 */
class PygmentsModulesTest
{
    @Test
    void modulesArePrecompiled() throws Exception
    {
        // Jython compiles the source of a module to a Java class when importing it, which is much slower than loading
        // a Java class compiled at build time. A module loaded from such a class has the path of that class.
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("python");
        engine.eval("""
            import sys
            from pygments.lexers._mapping import LEXERS
            import pygments.formatters.xdom
            import pygments.styles.xwiki_default
            for lexer in LEXERS.values():
                __import__(lexer[0])
            sources = sorted(name for name, module in sys.modules.items()
                if name.startswith('pygments') and module is not None
                and not getattr(module, '__file__', '').endswith('$py.class'))
            """);

        assertEquals(List.of(), engine.get("sources"));
    }
}
