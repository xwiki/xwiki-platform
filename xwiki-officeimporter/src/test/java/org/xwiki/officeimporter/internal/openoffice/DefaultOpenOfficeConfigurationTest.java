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
package org.xwiki.officeimporter.internal.openoffice;

import org.xwiki.officeimporter.internal.MockDocumentAccessBridge;
import org.xwiki.officeimporter.openoffice.OpenOfficeConfiguration;
import org.xwiki.rendering.scaffolding.AbstractRenderingTestCase;

/**
 * Test case for {@link DefaultOpenOfficeConfiguration}.
 * 
 * @version $Id$
 * @since 1.8RC3
 */
public class DefaultOpenOfficeConfigurationTest extends AbstractRenderingTestCase
{
    /**
     * Office importer configuration.
     */
    private OpenOfficeConfiguration configuration;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        this.configuration =
            (OpenOfficeConfiguration) getComponentManager()
                .lookup(OpenOfficeConfiguration.class, "default");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.test.AbstractXWikiComponentTestCase#registerComponents()
     */
    @Override
    protected void registerComponents() throws Exception
    {
        super.registerComponents();

        getComponentManager().registerComponent(MockDocumentAccessBridge.getComponentDescriptor());
    }

    /**
     * Test if default configuration values are present.
     */
    public void testDefaultConfiguration()
    {
        assertNotNull(configuration.getHomePath());
        assertNotNull(configuration.getProfilePath());
        assertTrue(configuration.getMaxTasksPerProcess() > 0);
        assertTrue(configuration.getTaskExecutionTimeout() > 0);
    }
}
