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
package org.xwiki.extension.index.internal.job;

import java.util.Arrays;

import javax.inject.Inject;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.namespace.Namespace;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.job.JobExecutor;

/**
 * Component in charge of scheduling and stopping indexing jobs.
 * 
 * @version $Id$
 * @since 12.10RC1
 */
@Component(roles = ExtensionIndexJobScheduler.class)
public class ExtensionIndexJobScheduler implements Disposable, Initializable
{
    @Inject
    private JobExecutor jobs;

    private final ExtensionIndexRequest request =
        new ExtensionIndexRequest(true, true, true, Arrays.asList(Namespace.ROOT));

    /**
     * @param namespace the namespace to add to the analysis
     */
    public void start(Namespace namespace)
    {
        this.request.addNamespace(namespace);

        // Queue an analysis of the namespace

    }

    /**
     * @param namespace the namespace to remove from the analysis
     */
    public void remove(Namespace namespace)
    {
        this.request.removeNamespace(namespace);

        // TODO: remove the namespace from the index
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {

    }
}
