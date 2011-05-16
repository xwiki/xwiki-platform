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
import org.xwiki.container.ApplicationContext;
import org.xwiki.container.Container;
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

        final ApplicationContext appContext = getMockery().mock(ApplicationContext.class);
        getMockery().checking(new Expectations() {{
            oneOf(appContext).getResource("/WEB-INF/xwiki.properties");
            will(returnValue(null));
        }});

        Container container = getComponentManager().lookup(Container.class);
        container.setApplicationContext(appContext);

        // Set a mock Logger to capture all log outputs and perform verifications
        this.logger = getMockery().mock(Logger.class);

        this.source = new XWikiPropertiesConfigurationSource();
        ReflectionUtils.setFieldValue(this.source, "container", container);
        ReflectionUtils.setFieldValue(this.source, "logger", this.logger);
    }

    @Test
    public void testInitializeWhenNoPropertiesFileAndDebugEnabled() throws Exception
    {
        getMockery().checking(new Expectations() {{
            oneOf(logger).isDebugEnabled();
            will(returnValue(true));
            // This is the test
            oneOf(logger).debug("No configuration file [/WEB-INF/xwiki.properties] found. "
                + "Using default configuration values.");
        }});

        this.source.initialize();
    }

    @Test
    public void testInitializeWhenNoPropertiesFileAndDebugNotEnabled() throws Exception
    {
        getMockery().checking(new Expectations() {{
            oneOf(logger).isDebugEnabled();
            will(returnValue(false));
            // This is the test. It shows nothing is logged when the properties file is not available.
        }});

        this.source.initialize();
    }
}
