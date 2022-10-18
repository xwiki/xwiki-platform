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
package org.xwiki.rendering.async;

import java.util.List;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.runner.RunWith;
import org.xwiki.environment.Environment;
import org.xwiki.job.Job;
import org.xwiki.job.JobExecutor;
import org.xwiki.job.Request;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;
import org.xwiki.rendering.async.internal.AsyncRendererJobRequest;
import org.xwiki.rendering.async.internal.AsyncRendererJobStatus;
import org.xwiki.rendering.test.integration.RenderingTestSuite;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.jmock.MockingComponentManager;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

/**
 * Run all tests found in {@code *.test} files located in the classpath. These {@code *.test} files must follow the
 * conventions described in {@link org.xwiki.rendering.test.integration.TestDataParser}.
 *
 * @version $Id$
 */
@RunWith(RenderingTestSuite.class)
@RenderingTestSuite.Scope(/* pattern = "macroasync3.test" */)
public class IntegrationTests
{
    @RenderingTestSuite.Initialized
    public void initialize(MockingComponentManager cm) throws Exception
    {
        Mockery mockery = new JUnit4Mockery();

        final Environment environment = cm.registerMockComponent(mockery, Environment.class, "default");
        final WikiDescriptorManager wikiDescriptorManager =
            cm.registerMockComponent(mockery, WikiDescriptorManager.class, "default");
        final ObservationManager observationManager =
            cm.registerMockComponent(mockery, ObservationManager.class, "default");
        final AsyncContext asyncContext = cm.registerMockComponent(mockery, AsyncContext.class, "default");
        final JobExecutor jobExecutor = cm.registerMockComponent(mockery, JobExecutor.class, "default");
        final AuthorizationManager authorization =
            cm.registerMockComponent(mockery, AuthorizationManager.class, "default");
        final Job job = mockery.mock(Job.class);
        final AsyncRendererJobRequest jobRequest = new AsyncRendererJobRequest();
        final AsyncRendererJobStatus jobStatus = new AsyncRendererJobStatus(jobRequest, observationManager, null);
        mockery.checking(new Expectations()
        {
            {
                allowing(environment).getResourceAsStream(with(any(String.class)));
                will(returnValue(null));
                allowing(wikiDescriptorManager).getCurrentWikiId();
                will(returnValue("wiki"));
                allowing(observationManager).notify(with(any(Event.class)), with(any(Object.class)));
                allowing(observationManager).notify(with(any(Event.class)), with(any(Object.class)),
                    with(any(Object.class)));
                allowing(asyncContext).isEnabled();
                will(returnValue(true));
                allowing(asyncContext).useEntity(with(any(EntityReference.class)));
                allowing(job).getStatus();
                will(returnValue(jobStatus));
                allowing(jobExecutor).execute(with(equal("asyncrenderer")), with(any(Request.class)));
                will(returnValue(job));
                allowing(jobExecutor).getJob(with(any(List.class)));
                will(returnValue(null));
                allowing(authorization).hasAccess(with(any(Right.class)), with(any(DocumentReference.class)),
                    with(any(EntityReference.class)));
                will(returnValue(true));
                allowing(authorization).checkAccess(with(any(Right.class)), with(any(DocumentReference.class)),
                    with(any(EntityReference.class)));
            }
        });
    }
}
