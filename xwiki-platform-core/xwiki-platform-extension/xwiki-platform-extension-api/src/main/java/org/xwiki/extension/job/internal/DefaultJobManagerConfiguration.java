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
package org.xwiki.extension.job.internal;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.environment.Environment;
import org.xwiki.extension.job.JobManagerConfiguration;

/**
 * Default implementation of {@link JobManagerConfiguration}.
 * 
 * @version $Id$
 */
@Component
@Singleton
public class DefaultJobManagerConfiguration implements JobManagerConfiguration
{
    /**
     * Used to get permanent directory.
     */
    @Inject
    private Environment environment;

    /**
     * The configuration.
     */
    @Inject
    private Provider<ConfigurationSource> configuration;

    // Cache

    /**
     * @see DefaultJobManagerConfiguration#getStorage()
     */
    private File store;

    /**
     * @return job manager home folder
     */
    private File getHome()
    {
        return new File(this.environment.getPermanentDirectory(), "jobs/");
    }

    @Override
    public File getStorage()
    {
        if (this.store == null) {
            String localRepositoryPath = this.configuration.get().getProperty("job.statusFolder");

            if (localRepositoryPath == null) {
                this.store = new File(getHome(), "status/");
            } else {
                this.store = new File(localRepositoryPath);
            }
        }

        return this.store;
    }
}
