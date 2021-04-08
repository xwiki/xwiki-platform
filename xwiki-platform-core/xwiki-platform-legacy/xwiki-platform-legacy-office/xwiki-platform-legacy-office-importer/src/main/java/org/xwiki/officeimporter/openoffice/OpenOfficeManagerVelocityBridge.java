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
package org.xwiki.officeimporter.openoffice;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.context.Execution;
import org.xwiki.officeimporter.server.script.OfficeServerScriptService;
import org.xwiki.script.service.ScriptService;

/**
 * A bridge between {@link OpenOfficeManager} and velocity scripts.
 * 
 * @version $Id$
 * @since 1.8RC3
 * @deprecated since 4.1M1 use the {@link ScriptService} with hint "officemanager" instead
 */
@Deprecated
public class OpenOfficeManagerVelocityBridge
{
    /**
     * The key used to place any error messages while trying to control the oo server instance.
     */
    public static final String OFFICE_MANAGER_ERROR = "OFFICE_MANAGER_ERROR";

    /**
     * The underlying script service.
     */
    private final ScriptService scriptService;

    /**
     * Creates a new {@link OpenOfficeManagerVelocityBridge} with the provided {@link OpenOfficeManager} component.
     * 
     * @param officeManager office manager component
     * @param documentAccessBridge document access bridge component
     * @param execution current execution
     */
    public OpenOfficeManagerVelocityBridge(OpenOfficeManager officeManager, DocumentAccessBridge documentAccessBridge,
        Execution execution)
    {
        // We don't cast to the actual implementation here because it complicates the unit test that verifies the
        // bridge initialization. See VelocityContextInitializerTest#testVelocityBridges().
        scriptService = (ScriptService) execution.getContext().getProperty("OpenOfficeManagerScriptService");
    }

    /**
     * Casts the {@link ScriptService} to its actual implementation so that we can call its methods.
     * 
     * @return the actual implementation of the script service
     */
    private OfficeServerScriptService getScriptService()
    {
        return (OfficeServerScriptService) scriptService;
    }

    /**
     * Tries to start the oo server process.
     * 
     * @return true if the operation succeeds, false otherwise.
     */
    public boolean startServer()
    {
        return getScriptService().startServer();
    }

    /**
     * Tries to stop the oo server process.
     * 
     * @return true if the operation succeeds, false otherwise.
     */
    public boolean stopServer()
    {
        return getScriptService().stopServer();
    }

    /**
     * @return current status of the oo server process as a string.
     */
    public String getServerState()
    {
        return getScriptService().getServerState();
    }

    /**
     * @return any error messages encountered.
     */
    public String getLastErrorMessage()
    {
        return getScriptService().getLastErrorMessage();
    }
}
