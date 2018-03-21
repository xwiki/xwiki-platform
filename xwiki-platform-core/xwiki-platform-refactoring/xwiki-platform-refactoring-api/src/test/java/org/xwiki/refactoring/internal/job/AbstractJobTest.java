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
package org.xwiki.refactoring.internal.job;

import org.junit.Before;
import org.xwiki.job.AbstractJob;
import org.xwiki.job.Job;
import org.xwiki.job.Request;
import org.xwiki.refactoring.internal.ModelBridge;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

/**
 * Base class for writing unit tests for refactoring jobs extending {@link AbstractJob}.
 *
 * @version $Id$
 * @since 9.4RC1
 */
public abstract class AbstractJobTest
{
    protected ModelBridge modelBridge;

    @Before
    public void configure() throws Exception
    {
        this.modelBridge = getMocker().getInstance(ModelBridge.class);
    }

    protected abstract MockitoComponentMockingRule<Job> getMocker();

    protected Job run(Request request) throws Throwable
    {
        Job job = getMocker().getComponentUnderTest();
        job.initialize(request);
        job.run();

        Throwable error = job.getStatus().getError();
        if (job.getStatus().getError() != null) {
            throw error;
        }

        return job;
    }
}
