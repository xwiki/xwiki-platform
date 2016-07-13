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
package org.xwiki.rendering.macro.test;

import java.io.IOException;

import javax.script.ScriptContext;
import javax.script.ScriptException;

/**
 * Test engine that output results in the writer.
 *
 * @version $Id$
 * @since 6.1
 */
public class Test1ScriptEngineFactory extends AbstractTestScriptEngineFactory
{
    public Test1ScriptEngineFactory()
    {
        super("test1", "Test1");
    }

    @Override
    public Object eval(String script, ScriptContext context) throws ScriptException
    {
        try {
            context.getWriter().write("Test " + script);
        } catch (IOException e) {
            throw new ScriptException("Failed to write");
        }

        return null;
    }
}
