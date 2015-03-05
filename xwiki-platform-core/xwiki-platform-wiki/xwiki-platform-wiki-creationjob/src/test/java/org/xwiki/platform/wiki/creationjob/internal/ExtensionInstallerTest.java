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
package org.xwiki.platform.wiki.creationjob.internal;

import java.util.Arrays;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.job.InstallRequest;
import org.xwiki.extension.job.internal.InstallJob;
import org.xwiki.job.Job;
import org.xwiki.job.Request;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.platform.wiki.creationjob.WikiCreationException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @version $Id$
 */
public class ExtensionInstallerTest
{
    @Rule
    public MockitoComponentMockingRule<ExtensionInstaller> mocker =
            new MockitoComponentMockingRule<>(ExtensionInstaller.class);
    
    @Test
    public void installExtension() throws Exception
    {
        // Mocks
        InstallJob installJob = mock(InstallJob.class);
        mocker.registerComponent(Job.class, InstallJob.JOBTYPE, installJob);
        final InstallRequest[] installRequest = {null};
        
        doAnswer(new Answer()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                installRequest[0] = (InstallRequest) invocation.getArguments()[0];
                return null;
            }
        }).when(installJob).initialize(any(Request.class));
        
        // Test
        mocker.getComponentUnderTest().installExtension("wikiId", new ExtensionId("extensionId", "version"));
        
        // Verify
        assertNotNull(installRequest[0]);
        assertEquals(Arrays.asList("wiki:wikiId"), installRequest[0].getNamespaces());
        assertEquals(Arrays.asList("wikicreation", "install", "wikiId"), installRequest[0].getId());
        assertEquals(Arrays.asList(new ExtensionId("extensionId", "version")), installRequest[0].getExtensions());
        assertEquals(new DocumentReference("xwiki", "XWiki", "superadmin"),
                installRequest[0].getProperty("user.reference"));
        verify(installJob).run();
    }

    @Test
    public void installExtensionWithException() throws Exception
    {
        // Test
        WikiCreationException caughtException = null;
        try {
            mocker.getComponentUnderTest().installExtension("wikiId", new ExtensionId("extensionId", "version"));
        } catch (WikiCreationException e) {
            caughtException = e;
        }
        
        // Verify
        assertNotNull(caughtException);
        assertEquals("Failed to install the extension [extensionId-version] on the wiki [wikiId].",
                caughtException.getMessage());
        assertTrue(caughtException.getCause() instanceof ComponentLookupException);
    }
}
