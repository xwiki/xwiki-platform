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

import org.jmock.Expectations;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.environment.Environment;
import org.xwiki.test.AbstractComponentTestCase;

/**
 * Unit tests for {@link XWikiPropertiesConfigurationSource}.
 *
 * @version $Id$
 * @since 3.0M1
 */
public class XWikiPropertiesConfigurationSourceTest extends AbstractComponentTestCase
{
    private XWikiPropertiesConfigurationSource source;

    private Logger logger;

    @Before
    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        final Environment environment = getMockery().mock(Environment.class);
        getMockery().checking(new Expectations() {{
            oneOf(environment).getResource("/WEB-INF/xwiki.properties");
            will(returnValue(null));
        }});

        // Set a mock Logger to capture all log outputs and perform verifications
        this.logger = getMockery().mock(Logger.class);

        this.source = new XWikiPropertiesConfigurationSource();
        ReflectionUtils.setFieldValue(this.source, "environment", environment);
        ReflectionUtils.setFieldValue(this.source, "logger", this.logger);
    }

    @Test
    public void testInitializeWhenNoPropertiesFile() throws Exception
    {
        getMockery().checking(new Expectations() {{
            // This is the test
            oneOf(logger).debug("No configuration file [{}] found. Using default configuration values.",
                "/WEB-INF/xwiki.properties");
        }});

        this.source.initialize();

        // Verifies that we can get a property from the source (i.e. that it's correctly initialized)
        this.source.getProperty("key");
    }
}
