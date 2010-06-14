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

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import java.io.StringWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
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

        Assert.assertEquals(1, data.values().size());
        CompositeData cd = ((CompositeData) data.values().iterator().next());
        Assert.assertEquals("<global>", cd.get("templateName"));
        Assert.assertEquals(0, ((String[])cd.get("macroNames")).length);

        StringWriter out = new StringWriter();
        engine.evaluate(new VelocityContext(), out, "testmacronamespace", "#macro(testmacro)#end");
        data = jmxBean.getTemplates();

        Assert.assertEquals(2, data.values().size());
        Map<String, String[]> retrievedData = new HashMap<String, String[]>();
        for (CompositeData cdata : (Collection<CompositeData>) data.values()) {
            retrievedData.put((String) cdata.get("templateName"), (String[]) cdata.get("macroNames"));
        }
        Assert.assertEquals(0, retrievedData.get("<global>").length);
        Assert.assertEquals(1, retrievedData.get("testmacronamespace").length);
        Assert.assertEquals("testmacro", retrievedData.get("testmacronamespace")[0]);
    }
}
