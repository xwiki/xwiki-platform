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
    }

    public void testInitializeConfiguration() throws Exception
    {
        DefaultConfigurationManager manager = new DefaultConfigurationManager();
        TestConfiguration configuration = new TestConfiguration();

        BaseConfiguration baseConfiguration = new BaseConfiguration();
        baseConfiguration.setProperty("field1", "test0");
        baseConfiguration.setProperty("prefix.field1", "test1");
        ConfigurationSource source = new CommonsConfigurationSource(baseConfiguration);

        manager.initializeConfiguration(configuration, Arrays.asList(source), "prefix");

        assertEquals("test1", configuration.getField1());
        assertEquals("DefaultValue", configuration.getField2());
    }
}
