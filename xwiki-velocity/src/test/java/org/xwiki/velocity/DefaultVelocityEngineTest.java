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
package org.xwiki.velocity;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Properties;

import org.xwiki.test.AbstractXWikiComponentTestCase;

/**
 * Unit tests for {@link DefaultVelocityEngine}.
 */
public class DefaultVelocityEngineTest extends AbstractXWikiComponentTestCase
{
    private VelocityEngine engine;

    protected void setUp() throws Exception
    {
        super.setUp();
        this.engine = (VelocityEngine) getComponentManager().lookup(VelocityEngine.ROLE);
    }

    public void testEvaluateReader() throws Exception
    {
        engine.initialize(new Properties());
        StringWriter writer = new StringWriter();
        engine.evaluate(new org.apache.velocity.VelocityContext(), writer, "mytemplate",
            new StringReader("#set($foo='hello')$foo World"));
        assertEquals("hello World", writer.toString());
    }

    public void testEvaluateString() throws Exception
    {
        engine.initialize(new Properties());
        StringWriter writer = new StringWriter();
        engine.evaluate(new org.apache.velocity.VelocityContext(), writer, "mytemplate",
            "#set($foo='hello')$foo World");
        assertEquals("hello World", writer.toString());
    }

    /**
     * Verify that the default configuration doesn't allow calling Class.forName.
     */
    public void testSecureUberspectorActiveByDefault() throws Exception
    {
        engine.initialize(new Properties());
        StringWriter writer = new StringWriter();
        engine.evaluate(new org.apache.velocity.VelocityContext(), writer, "mytemplate",
            "#set($foo = 'test')#set($object = $foo.class.forName('java.util.ArrayList')"
            + ".newInstance())$object.size()");
        assertEquals("$object.size()", writer.toString());
    }

    public void testOverrideConfiguration() throws Exception
    {
        // For example try setting a non secure Uberspector.
        Properties properties = new Properties();
        properties.setProperty("runtime.introspector.uberspect",
            "org.apache.velocity.util.introspection.UberspectImpl");
        engine.initialize(properties);
        StringWriter writer = new StringWriter();
        engine.evaluate(new org.apache.velocity.VelocityContext(), writer, "mytemplate",
            "#set($foo = 'test')#set($object = $foo.class.forName('java.util.ArrayList')"
            + ".newInstance())$object.size()");
        assertEquals("0", writer.toString());
    }
}
