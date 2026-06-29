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

import java.util.List;

import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.job.InstallRequest;
import org.xwiki.extension.job.internal.InstallJob;
import org.xwiki.job.Job;
import org.xwiki.job.Request;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.platform.wiki.creationjob.WikiCreationException;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @version $Id$
 */
@ComponentTest
class ExtensionInstallerTest
{
    @InjectMockComponents
    private ExtensionInstaller extensionInstaller;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @Test
    void installExtension() throws Exception
    {
        // Mocks
        InstallJob installJob = mock(InstallJob.class);
        this.componentManager.registerComponent(Job.class, InstallJob.JOBTYPE, installJob);
        final InstallRequest[] installRequest = {null};

        doAnswer(invocation -> {
            installRequest[0] = (InstallRequest) invocation.getArguments()[0];
            return null;
        }).when(installJob).initialize(any(Request.class));

        // Test
        this.extensionInstaller.installExtension("wikiId", new ExtensionId("extensionId", "version"));

        // Verify
        assertNotNull(installRequest[0]);
        assertEquals(List.of("wiki:wikiId"), installRequest[0].getNamespaces());
        assertEquals(List.of("wikicreation", "install", "wikiId"), installRequest[0].getId());
        assertEquals(List.of(new ExtensionId("extensionId", "version")), installRequest[0].getExtensions());
        assertEquals(new DocumentReference("xwiki", "XWiki", "superadmin"),
            installRequest[0].getProperty("user.reference"));
        verify(installJob).run();
    }

    @Test
    void installExtensionWithException() throws Exception
    {
        // Test and verify
        WikiCreationException caughtException = assertThrows(WikiCreationException.class,
            () -> this.extensionInstaller.installExtension("wikiId", new ExtensionId("extensionId", "version")));
        assertEquals("Failed to install the extension [extensionId/version] on the wiki [wikiId].",
            caughtException.getMessage());
        assertInstanceOf(ComponentLookupException.class, caughtException.getCause());
    }
}
