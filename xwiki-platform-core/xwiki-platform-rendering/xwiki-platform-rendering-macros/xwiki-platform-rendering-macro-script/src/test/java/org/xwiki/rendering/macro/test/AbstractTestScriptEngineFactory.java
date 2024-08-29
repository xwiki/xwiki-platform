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

import java.io.Reader;
import java.util.Arrays;
import java.util.List;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;

/**
 * For writing test engines.
 *
 * @version $Id$
 * @since 7.1M1
 */
public abstract class AbstractTestScriptEngineFactory implements ScriptEngineFactory, ScriptEngine
{
    private static final String VERSION = "1.0";

    private String shortName;

    private String languageName;

    public AbstractTestScriptEngineFactory(String shortName, String languageName)
    {
        this.shortName = shortName;
        this.languageName = languageName;
    }

    @Override
    public String getEngineName()
    {
        return this.shortName;
    }

    @Override
    public String getEngineVersion()
    {
        return VERSION;
    }

    @Override
    public List<String> getExtensions()
    {
        return null;
    }

    @Override
    public List<String> getMimeTypes()
    {
        return null;
    }

    @Override
    public List<String> getNames()
    {
        return Arrays.asList(this.shortName, this.languageName);
    }

    @Override
    public String getLanguageName()
    {
        return this.languageName;
    }

    @Override
    public String getLanguageVersion()
    {
        return VERSION;
    }

    @Override
    public Object getParameter(String key)
    {
        return null;
    }

    @Override
    public String getMethodCallSyntax(String obj, String m, String... args)
    {
        return null;
    }

    @Override
    public String getOutputStatement(String toDisplay)
    {
        return null;
    }

    @Override
    public String getProgram(String... statements)
    {
        return null;
    }

    @Override
    public ScriptEngine getScriptEngine()
    {
        return this;
    }

    @Override
    public Object eval(Reader reader, ScriptContext context) throws ScriptException
    {
        return null;
    }

    @Override
    public Object eval(String script) throws ScriptException
    {
        return null;
    }

    @Override
    public Object eval(Reader reader) throws ScriptException
    {
        return null;
    }

    @Override
    public Object eval(String script, Bindings n) throws ScriptException
    {
        return null;
    }

    @Override
    public Object eval(Reader reader, Bindings n) throws ScriptException
    {
        return null;
    }

    @Override
    public void put(String key, Object value)
    {
    }

    @Override
    public Object get(String key)
    {
        return null;
    }

    @Override
    public Bindings getBindings(int scope)
    {
        return null;
    }

    @Override
    public void setBindings(Bindings bindings, int scope)
    {
    }

    @Override
    public Bindings createBindings()
    {
        return null;
    }

    @Override
    public ScriptContext getContext()
    {
        return null;
    }

    @Override
    public void setContext(ScriptContext context)
    {
    }

    @Override
    public ScriptEngineFactory getFactory()
    {
        return null;
    }
}
