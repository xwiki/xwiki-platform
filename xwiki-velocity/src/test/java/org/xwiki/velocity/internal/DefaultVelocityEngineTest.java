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
 *
 */
package org.xwiki.velocity.internal;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.velocity.context.Context;
import org.xwiki.test.AbstractXWikiComponentTestCase;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.internal.DefaultVelocityEngine;

/**
 * Unit tests for {@link DefaultVelocityEngine}.
 */
public class DefaultVelocityEngineTest extends AbstractXWikiComponentTestCase
{
    private VelocityEngine engine;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        this.engine = getComponentManager().lookup(VelocityEngine.class);
    }

    public void testEvaluateReader() throws Exception
    {
        this.engine.initialize(new Properties());
        StringWriter writer = new StringWriter();
        this.engine.evaluate(new org.apache.velocity.VelocityContext(), writer, "mytemplate",
            new StringReader("#set($foo='hello')$foo World"));
        assertEquals("hello World", writer.toString());
    }

    public void testEvaluateString() throws Exception
    {
        this.engine.initialize(new Properties());
        StringWriter writer = new StringWriter();
        this.engine.evaluate(new org.apache.velocity.VelocityContext(), writer, "mytemplate",
            "#set($foo='hello')$foo World");
        assertEquals("hello World", writer.toString());
    }

    /**
     * Verify that the default configuration doesn't allow calling Class.forName.
     */
    public void testSecureUberspectorActiveByDefault() throws Exception
    {
        this.engine.initialize(new Properties());
        StringWriter writer = new StringWriter();
        this.engine.evaluate(new org.apache.velocity.VelocityContext(), writer, "mytemplate",
            "#set($foo = 'test')#set($object = $foo.class.forName('java.util.ArrayList')"
                + ".newInstance())$object.size()");
        assertEquals("$object.size()", writer.toString());
    }

    /**
     * Verify that the default configuration allows #setting existing variables to null.
     */
    public void testSettingNullAllowedByDefault() throws Exception
    {
        this.engine.initialize(new Properties());
        StringWriter writer = new StringWriter();
        Context context = new org.apache.velocity.VelocityContext();
        context.put("null", null);
        List<String> list = new ArrayList<String>();
        list.add("1");
        list.add(null);
        list.add("3");
        context.put("list", list);
        this.engine.evaluate(context, writer, "mytemplate", "#set($foo = true)${foo}#set($foo = $null)${foo}\n"
            + "#foreach($i in $list)${velocityCount}=$!{i} #end");
        assertEquals("true${foo}\n1=1 2= 3=3 ", writer.toString());
    }

    public void testOverrideConfiguration() throws Exception
    {
        // For example try setting a non secure Uberspector.
        Properties properties = new Properties();
        properties.setProperty("runtime.introspector.uberspect",
            "org.apache.velocity.util.introspection.UberspectImpl");
        this.engine.initialize(properties);
        StringWriter writer = new StringWriter();
        this.engine.evaluate(new org.apache.velocity.VelocityContext(), writer, "mytemplate",
            "#set($foo = 'test')#set($object = $foo.class.forName('java.util.ArrayList')"
                + ".newInstance())$object.size()");
        assertEquals("0", writer.toString());
    }

    public void testMacroIsolation() throws Exception
    {
        this.engine.initialize(new Properties());
        Context context = new org.apache.velocity.VelocityContext();
        this.engine.evaluate(context, new StringWriter(), "template1", "#macro(mymacro)test#end");
        StringWriter writer = new StringWriter();
        this.engine.evaluate(context, writer, "template2", "#mymacro");
        assertEquals("#mymacro", writer.toString());
    }

    public void testConfigureMacrosToBeGlobal() throws Exception
    {
        Properties properties = new Properties();
        // Force macros to be global
        properties.put("velocimacro.permissions.allow.inline.local.scope", "false");
        this.engine.initialize(properties);
        Context context = new org.apache.velocity.VelocityContext();
        this.engine.evaluate(context, new StringWriter(), "template1", "#macro(mymacro)test#end");
        StringWriter writer = new StringWriter();
        this.engine.evaluate(context, writer, "template2", "#mymacro");
        assertEquals("test", writer.toString());
    }
}
