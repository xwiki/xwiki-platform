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
import org.xwiki.officeimporter.openoffice.OpenOfficeManager;
import org.xwiki.rendering.scaffolding.AbstractRenderingTestCase;

/**
 * Test case for {@link DefaultOpenOfficeManager}.
 * 
 * @version $Id$
 * @since 1.8RC3
 */
public class DefaultOpenOfficeServerManagerTest extends AbstractRenderingTestCase
{
    /**
     * The {@link OpenOfficeManager} component.
     */
    private OpenOfficeManager oomanager;

    /**
     * The {@link OpenOfficeConfiguration} component.
     */
    private OpenOfficeConfiguration ooconfig;

    /**
     * {@inheritDoc}
     */
    protected void setUp() throws Exception
    {
        super.setUp();
        this.oomanager = (OpenOfficeManager) getComponentManager().lookup(OpenOfficeManager.class);
        this.ooconfig =
            (OpenOfficeConfiguration) getComponentManager().lookup(OpenOfficeConfiguration.class);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.test.AbstractXWikiComponentTestCase#registerComponents()
     */
    protected void registerComponents() throws Exception
    {
        super.registerComponents();
        getComponentManager().registerComponent(MockDocumentAccessBridge.getComponentDescriptor());
    }

    /**
     * Tests the initial status of the oo server manager.
     */
    public void testInitialStatus()
    {
        assertEquals(OpenOfficeConfiguration.SERVER_TYPE_INTERNAL, ooconfig.getServerType());
        assertEquals(8100, ooconfig.getServerPort());
        assertNotNull(ooconfig.getHomePath());
        assertNotNull(ooconfig.getProfilePath());
        assertEquals(OpenOfficeManager.ManagerState.NOT_CONNECTED, oomanager.getState());
    }
}
