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
package org.xwiki.extension.xar.script;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.job.Job;
import org.xwiki.script.service.ScriptService;

/**
 * Various XAR oriented APIs for scripts.
 * 
 * @version $Id$
 * @deprecated since 5.3M1, it's a sub {@link ScriptService} of the extension one now, see
 *             {@link XarExtensionScriptService}
 */
@Component
@Named("xarextension")
@Singleton
@Deprecated
public class DeprecatedXarExtensionScriptService implements ScriptService, Initializable
{
    @Inject
    @Named("extension.xar")
    private ScriptService service;

    private XarExtensionScriptService xarService;

    @Override
    public void initialize() throws InitializationException
    {
        this.xarService = (XarExtensionScriptService) this.service;
    }

    /**
     * Get the error generated while performing the previously called action.
     * 
     * @return an eventual exception or {@code null} if no exception was thrown
     */
    public Exception getLastError()
    {
        return this.xarService.getLastError();
    }

    /**
     * Make sure the provided XAR extension properly is registered in the installed extensions index.
     * <p>
     * Start an asynchronous Job.
     * 
     * @param id the extension identifier
     * @param version the extension version
     * @param wiki the wiki where the extension is installed
     * @return the {@link Job} object which can be used to monitor the progress of the installation process, or
     *         {@code null} in case of failure
     */
    public Job repairInstalledExtension(String id, String version, String wiki)
    {
        return this.xarService.repairInstalledExtension(id, version, wiki);
    }
}
