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

import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.container.ApplicationContext;
import org.xwiki.container.ApplicationContextListener;
import org.xwiki.officeimporter.openoffice.OpenOfficeServerConfiguration;
import org.xwiki.officeimporter.openoffice.OpenOfficeServerManager;
import org.xwiki.officeimporter.openoffice.OpenOfficeServerManagerException;

/**
 * {@link ApplicationContextListener} responsible for automatically starting openoffice server instance if required.
 * 
 * @version $Id$
 * @since 1.9M2
 */
public class OpenOfficeServerManagerApplicationContextListener extends AbstractLogEnabled implements
    ApplicationContextListener
{
    /**
     * The {@link OpenOfficeServerConfiguration} component.
     */
    private OpenOfficeServerConfiguration serverConfiguration;

    /**
     * The {@link OpenOfficeServerManager} component.
     */
    private OpenOfficeServerManager serverManager;

    /**
     * {@inheritDoc}
     */
    public void initializeApplicationContext(ApplicationContext applicationContext)
    {
        if (serverConfiguration.isAutoStart()) {
            try {
                serverManager.startServer();
            } catch (OpenOfficeServerManagerException ex) {
                getLogger().error(ex.getMessage(), ex);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void destroyApplicationContext(ApplicationContext applicationContext)
    {
        try {
            serverManager.stopServer();
        } catch (OpenOfficeServerManagerException ex) {
            getLogger().error(ex.getMessage(), ex);
        }
    }        
}
