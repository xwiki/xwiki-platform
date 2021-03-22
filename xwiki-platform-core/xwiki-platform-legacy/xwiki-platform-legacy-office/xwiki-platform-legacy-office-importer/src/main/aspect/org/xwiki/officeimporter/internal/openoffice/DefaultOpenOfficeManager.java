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

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.officeimporter.converter.OfficeConverter;
import org.xwiki.officeimporter.openoffice.OpenOfficeConverter;
import org.xwiki.officeimporter.openoffice.OpenOfficeManager;
import org.xwiki.officeimporter.openoffice.OpenOfficeManagerException;
import org.xwiki.officeimporter.server.OfficeServer;
import org.xwiki.officeimporter.server.OfficeServer.ServerState;
import org.xwiki.officeimporter.server.OfficeServerException;

/**
 * Default {@link OpenOfficeManager} implementation.
 * 
 * @version $Id$
 * @since 1.8RC3
 */
@Component
@Singleton
public class DefaultOpenOfficeManager implements OpenOfficeManager
{
    /**
     * The office server.
     */
    @Inject
    private OfficeServer officeServer;

    /**
     * The office document converter.
     */
    private OpenOfficeConverter converter;

    @Override
    public ManagerState getState()
    {
        ServerState serverState = officeServer.getState();
        if (serverState != null) {
            for (ManagerState managerState : ManagerState.values()) {
                if (managerState.ordinal() == serverState.ordinal()) {
                    return managerState;
                }
            }
        }
        return null;
    }

    @Override
    public void start() throws OpenOfficeManagerException
    {
        try {
            officeServer.start();
        } catch (OfficeServerException e) {
            throw new OpenOfficeManagerException(e.getMessage(), e.getCause());
        }
    }

    @Override
    public void stop() throws OpenOfficeManagerException
    {
        try {
            officeServer.stop();
        } catch (OfficeServerException e) {
            throw new OpenOfficeManagerException(e.getMessage(), e.getCause());
        }
    }

    @Override
    public OpenOfficeConverter getConverter()
    {
        if (converter == null) {
            OfficeConverter officeConverter = officeServer.getConverter();
            if (officeConverter != null) {
                converter = new DefaultOpenOfficeConverter(officeConverter);
            }
        }
        return converter;
    }
}
