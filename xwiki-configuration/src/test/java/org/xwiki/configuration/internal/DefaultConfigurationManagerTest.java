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
package org.xwiki.configuration.internal;

import junit.framework.TestCase;
import org.apache.commons.configuration.BaseConfiguration;
import org.xwiki.configuration.internal.commons.CommonsConfigurationSource;
import org.xwiki.configuration.internal.DefaultConfigurationManager;
import org.xwiki.configuration.ConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Unit tests for {@link org.xwiki.configuration.internal.DefaultConfigurationManagerTest}.
 *
 * @version $Id$
 * @since 1.6M1
 */
public class DefaultConfigurationManagerTest extends TestCase
{
    public class TestConfiguration
    {
        private String field1;
        private String field2 = "DefaultValue";
        private List<String> field3;
        private List<String> field4;
        private Properties field5;
        private Properties field6;
        
        public void setField1(String field)
        {
            this.field1 = field;
        }

        public String getField1()
        {
            return this.field1;
        }

        public void setField2(String field)
        {
            this.field2 = field;
        }

        public String getField2()
        {
            return this.field2;
        }
        
        public List<String> getField3()
        {
            return this.field3;
        }
        
        public void setField3(List<String> list)
        {
            this.field3 = list;
        }

        public List<String> getField4()
        {
            return this.field4;
        }
        
        public void setField4(List<String> list)
        {
            this.field4 = list;
        }

        public Properties getField5()
        {
            return this.field5;
        }
        
        public void setField5(Properties properties)
        {
            this.field5 = properties;
        }

        public Properties getField6()
        {
            return this.field6;
        }
        
        public void setField6(Properties properties)
        {
            this.field6 = properties;
        }
    }

    public void testInitializeConfiguration() throws Exception
    {
        DefaultConfigurationManager manager = new DefaultConfigurationManager();
        TestConfiguration configuration = new TestConfiguration();

        BaseConfiguration baseConfiguration = new BaseConfiguration();
        
        // Verify that a property without the correct prefix is ignored
        baseConfiguration.setProperty("field1", "test0");
        
        // Simple property
        baseConfiguration.setProperty("prefix.field1", "test1");
        
        // List property
        baseConfiguration.addProperty("prefix.field3", "value1");
        baseConfiguration.addProperty("prefix.field3", "value2");
        baseConfiguration.setProperty("prefix.field4", "value1, value2");
        
        // List property with properties inside
        baseConfiguration.addProperty("prefix.field5", "property1=value1");
        baseConfiguration.addProperty("prefix.field5", "property2=value2");
        baseConfiguration.setProperty("prefix.field6", "property1=value1, property2=value2");
        
        ConfigurationSource source = new CommonsConfigurationSource(baseConfiguration);

        manager.initializeConfiguration(configuration, Arrays.asList(source), "prefix");

        assertEquals("test1", configuration.getField1());
        assertEquals("DefaultValue", configuration.getField2());
        assertEquals(2, configuration.getField3().size());
        assertEquals("value1", configuration.getField3().get(0));
        assertEquals("value2", configuration.getField3().get(1));
        assertEquals(2, configuration.getField4().size());
        assertEquals("value1", configuration.getField4().get(0));
        assertEquals("value2", configuration.getField4().get(1));
        assertEquals(2, configuration.getField5().size());
        assertEquals("value1", configuration.getField5().getProperty("property1"));
        assertEquals("value2", configuration.getField5().getProperty("property2"));
        assertEquals(2, configuration.getField6().size());
        assertEquals("value1", configuration.getField6().getProperty("property1"));
        assertEquals("value2", configuration.getField6().getProperty("property2"));
    }
}
