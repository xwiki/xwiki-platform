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
package org.xwiki.velocity.introspection;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Date;
import java.util.Properties;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.util.introspection.SecureUberspector;
import org.apache.velocity.util.introspection.UberspectImpl;
import org.jmock.Mock;
import org.jmock.core.stub.DefaultResultStub;
import org.xwiki.component.logging.Logger;
import org.xwiki.test.AbstractXWikiComponentTestCase;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.internal.DefaultVelocityEngine;

/**
 * Unit tests for {@link ChainingUberspector}.
 */
public class ChainingUberspectorTest extends AbstractXWikiComponentTestCase
{
    private DefaultVelocityEngine engine;

    private Mock mockLogger;
    
    protected void setUp() throws Exception
    {
        super.setUp();
        this.engine = (DefaultVelocityEngine) getComponentManager().lookup(VelocityEngine.class);
        
        this.mockLogger = mock(Logger.class);
        this.mockLogger.setDefaultStub(new DefaultResultStub());
        this.engine.enableLogging((Logger) this.mockLogger.proxy());
    }

    /*
     * Tests that the uberspectors in the chain are called, and without a real uberspector no
     * methods are found.
     */
    public void testEmptyChain() throws Exception
    {
        Properties prop = new Properties();
        prop.setProperty(RuntimeConstants.UBERSPECT_CLASSNAME, ChainingUberspector.class
            .getCanonicalName());
        prop.setProperty(ChainingUberspector.UBERSPECT_CHAIN_CLASSNAMES, TestingUberspector.class
            .getCanonicalName());
        TestingUberspector.methodCalls = 0;
        engine.initialize(prop);
        StringWriter writer = new StringWriter();
        engine.evaluate(new org.apache.velocity.VelocityContext(), writer, "mytemplate",
            new StringReader("#set($foo = 'hello')#set($bar = $foo.toString())$bar"));
        assertEquals("$bar", writer.toString());
        assertEquals(1, TestingUberspector.methodCalls);
    }

    /*
     * Tests that using several uberspectors in the chain works, and methods are correctly found by
     * the last uberspector in the chain.
     */
    public void testBasicChaining() throws Exception
    {
        Properties prop = new Properties();
        prop.setProperty(RuntimeConstants.UBERSPECT_CLASSNAME, ChainingUberspector.class
            .getCanonicalName());
        prop.setProperty(ChainingUberspector.UBERSPECT_CHAIN_CLASSNAMES, UberspectImpl.class
            .getCanonicalName()
            + "," + TestingUberspector.class.getCanonicalName());
        TestingUberspector.methodCalls = 0;
        TestingUberspector.getterCalls = 0;
        engine.initialize(prop);
        StringWriter writer = new StringWriter();
        engine.evaluate(new org.apache.velocity.VelocityContext(), writer, "mytemplate",
            new StringReader("#set($foo = 'hello')#set($bar = $foo.toString())$bar"));
        assertEquals("hello", writer.toString());
        assertEquals(1, TestingUberspector.methodCalls);
        assertEquals(0, TestingUberspector.getterCalls);
    }

    /*
     * Tests that invalid uberspectors classnames are ignored.
     */
    public void testInvalidUberspectorsAreIgnored() throws Exception
    {
        Properties prop = new Properties();
        prop.setProperty(RuntimeConstants.UBERSPECT_CLASSNAME, ChainingUberspector.class
            .getCanonicalName());
        prop.setProperty(ChainingUberspector.UBERSPECT_CHAIN_CLASSNAMES, UberspectImpl.class
            .getCanonicalName()
            + ","
            + AbstractChainableUberspector.class.getCanonicalName()
            + ","
            + InvalidUberspector.class.getCanonicalName()
            + ","
            + TestingUberspector.class.getCanonicalName() + "," + Date.class.getCanonicalName());
        TestingUberspector.methodCalls = 0;
        InvalidUberspector.methodCalls = 0;
        engine.initialize(prop);
        StringWriter writer = new StringWriter();
        engine.evaluate(new org.apache.velocity.VelocityContext(), writer, "mytemplate",
            new StringReader("#set($foo = 'hello')#set($bar = $foo.toString())$bar"));
        assertEquals("hello", writer.toString());
        assertEquals(1, TestingUberspector.methodCalls);
        assertEquals(0, InvalidUberspector.methodCalls);
    }

    /*
     * Tests that a non-chainable entry in the chain does not forward calls.
     */
    public void testChainBreakingOnNonChainableEntry() throws Exception
    {
        Properties prop = new Properties();
        prop.setProperty(RuntimeConstants.UBERSPECT_CLASSNAME, ChainingUberspector.class
            .getCanonicalName());
        prop.setProperty(ChainingUberspector.UBERSPECT_CHAIN_CLASSNAMES, TestingUberspector.class
            .getCanonicalName()
            + "," + UberspectImpl.class.getCanonicalName());
        TestingUberspector.methodCalls = 0;
        engine.initialize(prop);
        StringWriter writer = new StringWriter();
        engine.evaluate(new org.apache.velocity.VelocityContext(), writer, "mytemplate",
            new StringReader("#set($foo = 'hello')#set($bar = $foo.toString())$bar"));
        assertEquals("hello", writer.toString());
        assertEquals(0, TestingUberspector.methodCalls);
    }

    /*
     * Checks that the default (non-secure) uberspector works and allows calling restricted methods.
     */
    public void testDefaultUberspectorWorks() throws Exception
    {
        Properties prop = new Properties();
        prop.setProperty(RuntimeConstants.UBERSPECT_CLASSNAME, ChainingUberspector.class
            .getCanonicalName());
        prop.setProperty(ChainingUberspector.UBERSPECT_CHAIN_CLASSNAMES, UberspectImpl.class
            .getCanonicalName());
        engine.initialize(prop);
        StringWriter writer = new StringWriter();
        engine.evaluate(new org.apache.velocity.VelocityContext(), writer, "mytemplate",
            new StringReader("#set($foo = 'hello')"
                + "#set($bar = $foo.getClass().getConstructors())$bar"));
        assertTrue(writer.toString().startsWith("[Ljava.lang.reflect.Constructor"));
    }

    /*
     * Checks that the secure uberspector works and does not allow calling restricted methods.
     */
    public void testSecureUberspectorWorks() throws Exception
    {
        Properties prop = new Properties();
        prop.setProperty(RuntimeConstants.UBERSPECT_CLASSNAME, ChainingUberspector.class
            .getCanonicalName());
        prop.setProperty(ChainingUberspector.UBERSPECT_CHAIN_CLASSNAMES, SecureUberspector.class
            .getCanonicalName());
        engine.initialize(prop);
        StringWriter writer = new StringWriter();
        engine.evaluate(new org.apache.velocity.VelocityContext(), writer, "mytemplate",
            new StringReader("#set($foo = 'hello')"
                + "#set($bar = $foo.getClass().getConstructors())$foo$bar"));
        assertEquals("hello$bar", writer.toString());
    }

    /*
     * Checks that when the chain property is not configured, by default the secure ubespector is
     * used.
     */
    public void testSecureUberspectorEnabledByDefault() throws Exception
    {
        Properties prop = new Properties();
        prop.setProperty(RuntimeConstants.UBERSPECT_CLASSNAME, ChainingUberspector.class
            .getCanonicalName());
        prop.setProperty(ChainingUberspector.UBERSPECT_CHAIN_CLASSNAMES, "");
        engine.initialize(prop);
        StringWriter writer = new StringWriter();
        engine.evaluate(new org.apache.velocity.VelocityContext(), writer, "mytemplate",
            new StringReader("#set($foo = 'hello')"
                + "#set($bar = $foo.getClass().getConstructors())$foo$bar"));
        assertEquals("hello$bar", writer.toString());
    }

    /*
     * Checks that the deprecated check uberspector works.
     */
    @SuppressWarnings("deprecation")
    public void testDeprecatedUberspector() throws Exception
    {
        Properties prop = new Properties();
        prop.setProperty(RuntimeConstants.UBERSPECT_CLASSNAME, ChainingUberspector.class
            .getCanonicalName());
        prop.setProperty(ChainingUberspector.UBERSPECT_CHAIN_CLASSNAMES, UberspectImpl.class
            .getCanonicalName()
            + ","
            + TestingUberspector.class.getCanonicalName()
            + ","
            + DeprecatedCheckUberspector.class.getCanonicalName());
        TestingUberspector.methodCalls = 0;
        TestingUberspector.getterCalls = 0;
        engine.initialize(prop);
        StringWriter writer = new StringWriter();
        VelocityContext context = new org.apache.velocity.VelocityContext();
        Date d = new Date();
        context.put("date", d);

        // Define expectations on the Logger
        this.mockLogger.expects(once()).method("warn").with(eq(
            "Deprecated usage of method [java.util.Date.getYear] in mytemplate@1,19"));
        this.mockLogger.expects(once()).method("warn").with(eq(
            "Deprecated usage of getter [java.util.Date.getMonth] in mytemplate@1,40"));

        engine.evaluate(context, writer, "mytemplate",
            new StringReader("#set($foo = $date.getYear())$foo $date.month"));

        assertEquals(d.getYear() + " " + d.getMonth(), writer.toString());
        assertEquals(1, TestingUberspector.methodCalls);
        assertEquals(1, TestingUberspector.getterCalls);
    }
}
