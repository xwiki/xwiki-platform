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
package org.xwiki.velocity.internal.jmx;

import org.apache.velocity.VelocityContext;
import org.junit.Assert;
import org.junit.Test;
import org.xwiki.test.AbstractComponentTestCase;
import org.xwiki.velocity.VelocityEngine;

import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import java.io.StringWriter;
import java.util.Properties;

/**
 * Unit tests for {@link JMXVelocityEngine}.
 * 
 * @version $Id$
 * @since 2.4M2
 */
public class JMXVelocityEngineTest extends AbstractComponentTestCase
{
    @Test
    public void testGetTemplates() throws Exception
    {
        VelocityEngine engine = getComponentManager().lookup(VelocityEngine.class);
        engine.initialize(new Properties());
        JMXVelocityEngine jmxBean = new JMXVelocityEngine(engine);

        TabularData data = jmxBean.getTemplates();

        ArrayType macroNameType = new ArrayType(1, SimpleType.STRING);
        String[] columnNames = new String[] {"templateName", "macroNames"};
        String[] descriptions = new String[] {"The Template Name (namespace)", "The names of registered Macros"};
        CompositeType rowType = new CompositeType("template",
            "Template management data (namespaces, macros) for a row", columnNames, descriptions,
            new OpenType[]{SimpleType.STRING, macroNameType});

        Assert.assertEquals(1, data.values().size());
        Assert.assertTrue("Found unexpected result: " + data.toString(), data.containsValue(
            new CompositeDataSupport(rowType, columnNames, new Object[]{"<global>", new String[]{}})));

        StringWriter out = new StringWriter();
        engine.evaluate(new VelocityContext(), out, "testmacronamespace", "#macro(testmacro)#end");
        data = jmxBean.getTemplates();

        Assert.assertEquals(2, data.values().size());
        Assert.assertTrue("Found unexpected result: " + data.toString(), data.containsValue(
            new CompositeDataSupport(rowType, columnNames, new Object[]{"<global>", new String[]{}})));
        Assert.assertTrue("Found unexpected result: " + data.toString(), data.containsValue(
            new CompositeDataSupport(rowType, columnNames,
                new Object[]{"testmacronamespace", new String[]{"testmacro"}})));
    }
}
