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
package org.xwiki.extension.test;

import java.util.List;

import org.junit.Before;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.job.InstallRequest;
import org.xwiki.extension.job.Job;
import org.xwiki.extension.job.JobManager;
import org.xwiki.extension.job.UninstallRequest;
import org.xwiki.extension.repository.LocalExtensionRepository;
import org.xwiki.logging.LogLevel;
import org.xwiki.logging.event.LogEvent;
import org.xwiki.test.AbstractComponentTestCase;

public abstract class AbstractExtensionHandlerTest extends AbstractComponentTestCase
{
    private LocalExtensionRepository localExtensionRepository;

    private RepositoryUtil repositoryUtil;

    private JobManager jobManager;

    @Before
    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        this.repositoryUtil =
            new RepositoryUtil(getClass().getSimpleName(), getConfigurationSource(), getComponentManager());
        this.repositoryUtil.setup();

        // lookup

        this.jobManager = getComponentManager().lookup(JobManager.class);
        this.localExtensionRepository = getComponentManager().lookup(LocalExtensionRepository.class);
    }

    @Override
    protected void registerComponents() throws Exception
    {
        super.registerComponents();

        registerComponent(ConfigurableDefaultCoreExtensionRepository.class);
    }

    protected LocalExtension install(ExtensionId extensionId) throws Throwable
    {
        return install(extensionId, null);
    }

    protected LocalExtension install(ExtensionId extensionId, String namespace) throws Throwable
    {
        InstallRequest installRequest = new InstallRequest();
        installRequest.addExtension(extensionId);
        if (namespace != null) {
            installRequest.addNamespace(namespace);
        }
        Job installJob = this.jobManager.install(installRequest);

        List<LogEvent> errors = installJob.getStatus().getLog(LogLevel.ERROR);
        if (!errors.isEmpty()) {
            throw errors.get(0).getThrowable();
        }

        return (LocalExtension) this.localExtensionRepository.resolve(extensionId);
    }

    protected LocalExtension uninstall(ExtensionId extensionId) throws Throwable
    {
        return uninstall(extensionId, null);
    }

    protected LocalExtension uninstall(ExtensionId extensionId, String namespace) throws Throwable
    {
        UninstallRequest uninstallRequest = new UninstallRequest();
        uninstallRequest.addExtension(extensionId);
        if (namespace != null) {
            uninstallRequest.addNamespace(namespace);
        }
        Job uninstallJob = this.jobManager.uninstall(uninstallRequest);

        List<LogEvent> errors = uninstallJob.getStatus().getLog(LogLevel.ERROR);
        if (!errors.isEmpty()) {
            throw errors.get(0).getThrowable();
        }

        return (LocalExtension) this.localExtensionRepository.resolve(extensionId);
    }
}
