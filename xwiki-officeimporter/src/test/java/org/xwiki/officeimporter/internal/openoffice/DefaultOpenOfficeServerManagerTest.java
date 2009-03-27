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
import org.xwiki.officeimporter.openoffice.OpenOfficeServerManager;

import org.xwiki.test.AbstractXWikiComponentTestCase;

/**
 * Test case for {@link DefaultOpenOfficeServerManager}.
 * 
 * @version $Id$
 * @since 1.8RC3
 */
public class DefaultOpenOfficeServerManagerTest extends AbstractXWikiComponentTestCase
{
    /**
     * The {@link OpenOfficeServerManager} component.
     */
    private OpenOfficeServerManager manager;

    /**
     * {@inheritDoc}
     */
    protected void setUp() throws Exception
    {
        getComponentManager().registerComponent(MockDocumentAccessBridge.getComponentDescriptor());
        super.setUp();
        manager = (OpenOfficeServerManager) getComponentManager().lookup(OpenOfficeServerManager.ROLE, "default");
    }
    
    /**
     * Tests the initial status of the oo server manager.
     */
    public void testInitialStatus()
    {
        assertNotNull(manager.getOfficeHome());
        assertNotNull(manager.getOfficeProfile());
        assertEquals(OpenOfficeServerManager.ServerState.NOT_RUNNING, manager.getServerState());
    }
}
