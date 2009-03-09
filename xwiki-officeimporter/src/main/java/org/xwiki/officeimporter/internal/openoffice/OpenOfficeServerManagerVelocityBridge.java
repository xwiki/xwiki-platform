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

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.officeimporter.openoffice.OpenOfficeServerManager;
import org.xwiki.officeimporter.openoffice.OpenOfficeServerManagerException;

/**
 * A bridge between {@link OpenOfficeServerManager} and velocity scripts.
 * 
 * @version $Id$
 * @since 1.8RC3
 */
public class OpenOfficeServerManagerVelocityBridge
{
    /**
     * The {@link OpenOfficeServerManager} component.
     */
    private OpenOfficeServerManager oomanager;

    /**
     * The {@link DocumentAccessBridge} component.
     */
    private DocumentAccessBridge docBridge;

    /**
     * Holds any error messages thrown during operations.
     */
    private String lastErrorMessage;

    /**
     * Creates a new {@link OpenOfficeServerManagerVelocityBridge} with the provided {@link OpenOfficeServerManager}
     * component.
     */
    public OpenOfficeServerManagerVelocityBridge(OpenOfficeServerManager oomanager, DocumentAccessBridge docBridge)
    {
        this.oomanager = oomanager;
        this.docBridge = docBridge;
    }

    /**
     * Tries to start the oo server process.
     * 
     * @return true if the operation succeeds, false otherwise.
     */
    public boolean startServer()
    {
        boolean success = false;
        if (docBridge.hasProgrammingRights()) {
            try {
                oomanager.startServer();
                success = true;
            } catch (OpenOfficeServerManagerException ex) {
                this.lastErrorMessage = ex.getMessage();
            }
        } else {
            this.lastErrorMessage = "Inadequate privileges.";
        }
        return success;
    }

    /**
     * Tries to stop the oo server process.
     * 
     * @return true if the operation succeeds, false otherwise.
     */
    public boolean stopServer()
    {
        boolean success = false;
        if (docBridge.hasProgrammingRights()) {
            try {
                oomanager.stopServer();
                success = true;
            } catch (OpenOfficeServerManagerException ex) {
                this.lastErrorMessage = ex.getMessage();
            }
        } else {
            this.lastErrorMessage = "Inadequate privileges.";
        }
        return success;
    }

    /**
     * @return path to openoffice server installation.
     */
    public String getOfficeHome()
    {
        return oomanager.getOfficeHome();
    }

    /**
     * @return path to openoffice execution profile.
     */
    public String getOfficeProfile()
    {
        return oomanager.getOfficeProfile();
    }

    /**
     * @return current status of the oo server process as a string.
     */
    public String getServerState()
    {
        return oomanager.getServerState().getDescription();
    }

    /**
     * @return any error messages encountered.
     */
    public String getLastErrorMessage()
    {
        return this.lastErrorMessage;
    }
}
