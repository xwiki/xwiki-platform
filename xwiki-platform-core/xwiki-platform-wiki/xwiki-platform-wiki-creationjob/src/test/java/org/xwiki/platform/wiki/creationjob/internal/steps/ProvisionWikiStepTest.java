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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
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
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.provisioning.WikiCopier;

import com.xpn.xwiki.XWikiContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 */
public class ProvisionWikiStepTest
{
    @Rule
    public MockitoComponentMockingRule<ProvisionWikiStep> mocker =
            new MockitoComponentMockingRule<>(ProvisionWikiStep.class);

    private WikiCopier wikiCopier;

    private ExtensionInstaller extensionInstaller;

    private ObservationManager observationManager;

    private Provider<XWikiContext> xcontextProvider;

    private XWikiContext xcontext;

    @Before
    public void setUp() throws Exception
    {
        wikiCopier = mocker.getInstance(WikiCopier.class);
        extensionInstaller = mocker.getInstance(ExtensionInstaller.class);
        observationManager = mocker.getInstance(ObservationManager.class);
        xcontextProvider = mocker.registerMockComponent(XWikiContext.TYPE_PROVIDER);
        xcontext = mock(XWikiContext.class);
        when(xcontextProvider.get()).thenReturn(xcontext);
    }

    @Test
    public void executeWhenSourceIsExtension() throws Exception
    {
        WikiCreationRequest request = new WikiCreationRequest();
        request.setWikiId("wikiId");
        request.setWikiSource(WikiSource.EXTENSION);
        ExtensionId extensionId = new ExtensionId("id", "version");
        request.setExtensionId(extensionId);

        // Test
        mocker.getComponentUnderTest().execute(request);

        // Verify
        verify(extensionInstaller).installExtension(eq("wikiId"), eq(extensionId));
        verify(observationManager).notify(eq(new WikiProvisioningEvent("wikiId")), eq("wikiId"), eq(xcontext));
        verify(observationManager).notify(eq(new WikiProvisionedEvent("wikiId")), eq("wikiId"), eq(xcontext));
        verifyZeroInteractions(wikiCopier);
    }

    @Test
    public void executeWhenSourceIsTemplate() throws Exception
    {
        WikiCreationRequest request = new WikiCreationRequest();
        request.setWikiId("wikiId");
        request.setWikiSource(WikiSource.TEMPLATE);
        request.setTemplateId("template");

        // Test
        mocker.getComponentUnderTest().execute(request);

        // Verify
        verify(wikiCopier).copyDocuments(eq("template"), eq("wikiId"), eq(false));
        verify(observationManager).notify(eq(new WikiProvisioningEvent("wikiId")), eq("wikiId"), eq(xcontext));
        verify(observationManager).notify(eq(new WikiCopiedEvent("template", "wikiId")), eq("template"), eq(xcontext));
        verify(observationManager).notify(eq(new WikiProvisionedEvent("wikiId")), eq("wikiId"), eq(xcontext));
        verifyZeroInteractions(extensionInstaller);
    }

    @Test
    public void executeWhenException() throws Exception
    {
        WikiCreationRequest request = new WikiCreationRequest();
        request.setWikiId("wikiId");
        request.setWikiSource(WikiSource.EXTENSION);
        ExtensionId extensionId = new ExtensionId("id", "version");
        request.setExtensionId(extensionId);
        
        // Mocks
        WikiCreationException exception = new WikiCreationException("Exception in ExtensionInstaller");
        doThrow(exception).when(extensionInstaller).installExtension("wikiId", extensionId);

        // Test
        WikiCreationException caughtException = null;
        try {
            mocker.getComponentUnderTest().execute(request);    
        } catch (WikiCreationException e) {
            caughtException = e;
        }

        // Verify
        assertNotNull(caughtException);
        assertEquals("Failed to provision the wiki [wikiId].", caughtException.getMessage());
        assertEquals(exception, caughtException.getCause());
        verify(observationManager).notify(eq(new WikiProvisioningEvent("wikiId")), eq("wikiId"), eq(xcontext));
        verify(observationManager).notify(eq(new WikiProvisioningFailedEvent("wikiId")), eq("wikiId"), eq(xcontext));
    }

    @Test
    public void getOrder() throws Exception
    {
        assertEquals(3000, mocker.getComponentUnderTest().getOrder());
    }
}
