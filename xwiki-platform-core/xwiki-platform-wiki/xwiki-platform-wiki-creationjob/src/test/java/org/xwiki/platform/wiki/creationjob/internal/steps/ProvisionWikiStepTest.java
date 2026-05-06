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
package org.xwiki.platform.wiki.creationjob.internal.steps;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.bridge.event.WikiCopiedEvent;
import org.xwiki.bridge.event.WikiProvisionedEvent;
import org.xwiki.bridge.event.WikiProvisioningEvent;
import org.xwiki.bridge.event.WikiProvisioningFailedEvent;
import org.xwiki.extension.ExtensionId;
import org.xwiki.observation.ObservationManager;
import org.xwiki.platform.wiki.creationjob.WikiCreationException;
import org.xwiki.platform.wiki.creationjob.WikiCreationRequest;
import org.xwiki.platform.wiki.creationjob.WikiSource;
import org.xwiki.platform.wiki.creationjob.internal.ExtensionInstaller;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.provisioning.WikiCopier;

import com.xpn.xwiki.XWikiContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 */
@ComponentTest
class ProvisionWikiStepTest
{
    @InjectMockComponents
    private ProvisionWikiStep provisionWikiStep;

    @MockComponent
    private WikiCopier wikiCopier;

    @MockComponent
    private ExtensionInstaller extensionInstaller;

    @MockComponent
    private ObservationManager observationManager;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    private XWikiContext xcontext;

    @BeforeEach
    void setUp()
    {
        this.xcontext = mock(XWikiContext.class);
        when(this.xcontextProvider.get()).thenReturn(this.xcontext);
    }

    @Test
    void executeWhenSourceIsExtension() throws Exception
    {
        WikiCreationRequest request = new WikiCreationRequest();
        request.setWikiId("wikiId");
        request.setWikiSource(WikiSource.EXTENSION);
        ExtensionId extensionId = new ExtensionId("id", "version");
        request.setExtensionId(extensionId);

        // Test
        this.provisionWikiStep.execute(request);

        // Verify
        verify(this.extensionInstaller).installExtension(eq("wikiId"), eq(extensionId));
        verify(this.observationManager).notify(eq(new WikiProvisioningEvent("wikiId")), eq("wikiId"), eq(this.xcontext));
        verify(this.observationManager).notify(eq(new WikiProvisionedEvent("wikiId")), eq("wikiId"), eq(this.xcontext));
        verifyNoInteractions(this.wikiCopier);
    }

    @Test
    void executeWhenSourceIsTemplate() throws Exception
    {
        WikiCreationRequest request = new WikiCreationRequest();
        request.setWikiId("wikiId");
        request.setWikiSource(WikiSource.TEMPLATE);
        request.setTemplateId("template");

        // Test
        this.provisionWikiStep.execute(request);

        // Verify
        verify(this.wikiCopier).copyDocuments(eq("template"), eq("wikiId"), eq(false));
        verify(this.observationManager).notify(eq(new WikiProvisioningEvent("wikiId")), eq("wikiId"), eq(this.xcontext));
        verify(this.observationManager).notify(eq(new WikiCopiedEvent("template", "wikiId")), eq("template"),
            eq(this.xcontext));
        verify(this.observationManager).notify(eq(new WikiProvisionedEvent("wikiId")), eq("wikiId"), eq(this.xcontext));
        verifyNoInteractions(this.extensionInstaller);
    }

    @Test
    void executeWhenException() throws Exception
    {
        WikiCreationRequest request = new WikiCreationRequest();
        request.setWikiId("wikiId");
        request.setWikiSource(WikiSource.EXTENSION);
        ExtensionId extensionId = new ExtensionId("id", "version");
        request.setExtensionId(extensionId);

        // Mocks
        WikiCreationException exception = new WikiCreationException("Exception in ExtensionInstaller");
        doThrow(exception).when(this.extensionInstaller).installExtension("wikiId", extensionId);

        // Test and verify
        WikiCreationException caughtException = assertThrows(WikiCreationException.class,
            () -> this.provisionWikiStep.execute(request));
        assertEquals("Failed to provision the wiki [wikiId].", caughtException.getMessage());
        assertEquals(exception, caughtException.getCause());
        verify(this.observationManager).notify(eq(new WikiProvisioningEvent("wikiId")), eq("wikiId"), eq(this.xcontext));
        verify(this.observationManager).notify(eq(new WikiProvisioningFailedEvent("wikiId")), eq("wikiId"),
            eq(this.xcontext));
    }

    @Test
    void getOrder()
    {
        assertEquals(3000, this.provisionWikiStep.getOrder());
    }
}
