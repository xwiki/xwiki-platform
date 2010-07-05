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
import org.apache.velocity.util.introspection.SecureUberspector;
import org.jmock.Expectations;
import org.junit.Assert;
import org.junit.Test;
import org.xwiki.test.AbstractMockingComponentTestCase;
import org.xwiki.test.annotation.MockingRequirement;
import org.xwiki.velocity.VelocityConfiguration;
import org.xwiki.velocity.introspection.ChainingUberspector;
import org.xwiki.velocity.introspection.DeprecatedCheckUberspector;

/**
 * Unit tests for {@link DefaultVelocityEngine}.
 */
public class DefaultVelocityEngineTest extends AbstractMockingComponentTestCase
{
    @MockingRequirement
    private DefaultVelocityEngine engine;

    @Override
    public void configure() throws Exception
    {
        final Properties properties = new Properties();
        properties.put("runtime.introspector.uberspect", ChainingUberspector.class.getName());
        properties.put("runtime.introspector.uberspect.chainClasses",
            SecureUberspector.class.getName() + ","  + DeprecatedCheckUberspector.class.getName());
        properties.put("directive.set.null.allowed", Boolean.TRUE.toString());
        properties.put("velocimacro.permissions.allow.inline.local.scope", Boolean.TRUE.toString());

        final VelocityConfiguration configuration = getComponentManager().lookup(VelocityConfiguration.class);
        getMockery().checking(new Expectations() {{
            oneOf(configuration).getProperties();
            will(returnValue(properties));
        }});
    }

    @Test
    public void testEvaluateReader() throws Exception
    {
        this.engine.initialize(new Properties());
        StringWriter writer = new StringWriter();
        this.engine.evaluate(new org.apache.velocity.VelocityContext(), writer, "mytemplate",
            new StringReader("#set($foo='hello')$foo World"));
        Assert.assertEquals("hello World", writer.toString());
    }

    @Test
    public void testEvaluateString() throws Exception
    {
        this.engine.initialize(new Properties());
        StringWriter writer = new StringWriter();
        this.engine.evaluate(new org.apache.velocity.VelocityContext(), writer, "mytemplate",
            "#set($foo='hello')$foo World");
        Assert.assertEquals("hello World", writer.toString());
    }

    /**
     * Verify that the default configuration doesn't allow calling Class.forName.
     */
    @Test
    public void testSecureUberspectorActiveByDefault() throws Exception
    {
        this.engine.initialize(new Properties());
        StringWriter writer = new StringWriter();
        this.engine.evaluate(new org.apache.velocity.VelocityContext(), writer, "mytemplate",
            "#set($foo = 'test')#set($object = $foo.class.forName('java.util.ArrayList')"
                + ".newInstance())$object.size()");
        Assert.assertEquals("$object.size()", writer.toString());
    }

    /**
     * Verify that the default configuration allows #setting existing variables to null.
     */
    @Test
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
        Assert.assertEquals("true${foo}\n1=1 2= 3=3 ", writer.toString());
    }

    @Test
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
        Assert.assertEquals("0", writer.toString());
    }

    @Test
    public void testMacroIsolation() throws Exception
    {
        this.engine.initialize(new Properties());
        Context context = new org.apache.velocity.VelocityContext();
        this.engine.evaluate(context, new StringWriter(), "template1", "#macro(mymacro)test#end");
        StringWriter writer = new StringWriter();
        this.engine.evaluate(context, writer, "template2", "#mymacro");
        Assert.assertEquals("#mymacro", writer.toString());
    }

    @Test
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
        Assert.assertEquals("test", writer.toString());
    }
}
