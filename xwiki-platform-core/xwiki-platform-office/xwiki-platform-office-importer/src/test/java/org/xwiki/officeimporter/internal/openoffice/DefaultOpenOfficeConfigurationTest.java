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

import org.junit.Assert;
import org.junit.Test;
import org.xwiki.officeimporter.internal.AbstractOfficeImporterTest;
import org.xwiki.officeimporter.openoffice.OpenOfficeConfiguration;

/**
 * Test case for {@link DefaultOpenOfficeConfiguration}.
 * 
 * @version $Id$
 * @since 1.8RC3
 */
public class DefaultOpenOfficeConfigurationTest extends AbstractOfficeImporterTest
{
    /**
     * Test if default configuration values are present.
     * 
     * @throws Exception if it fails to get the default {@link OpenOfficeConfiguration} implementation
     */
    @Test
    public void testDefaultConfiguration() throws Exception
    {
        OpenOfficeConfiguration configuration = getComponentManager().getInstance(OpenOfficeConfiguration.class);
        Assert.assertEquals(OpenOfficeConfiguration.SERVER_TYPE_INTERNAL, configuration.getServerType());
        Assert.assertEquals(8100, configuration.getServerPort());
        Assert.assertNull(configuration.getProfilePath());
        Assert.assertTrue(configuration.getMaxTasksPerProcess() > 0);
        Assert.assertTrue(configuration.getTaskExecutionTimeout() > 0);
    }
}
