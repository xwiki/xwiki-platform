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

import org.junit.jupiter.api.Test;
import org.xwiki.extension.ExtensionId;
import org.xwiki.platform.wiki.creationjob.WikiCreationException;
import org.xwiki.platform.wiki.creationjob.WikiCreationRequest;
import org.xwiki.platform.wiki.creationjob.WikiSource;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;
import org.xwiki.wiki.template.WikiTemplateManager;
import org.xwiki.wiki.user.MembershipType;
import org.xwiki.wiki.user.UserScope;
import org.xwiki.wiki.user.WikiUserManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 */
@ComponentTest
class SaveWikiMetaDataStepTest
{
    @InjectMockComponents
    private SaveWikiMetaDataStep saveWikiMetaDataStep;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    private WikiTemplateManager wikiTemplateManager;

    @MockComponent
    private WikiUserManager wikiUserManager;

    @Test
    void execute() throws Exception
    {
        WikiCreationRequest request = new WikiCreationRequest();
        request.setWikiId("wikiId");
        request.setDescription("description");
        request.setPrettyName("pretty name");
        request.setOwnerId("ownerId");
        request.setTemplate(false);
        request.setUserScope(UserScope.GLOBAL_ONLY);
        request.setMembershipType(MembershipType.INVITE);
        request.setWikiSource(WikiSource.EXTENSION);
        ExtensionId extensionId = new ExtensionId("id", "version");
        request.setExtensionId(extensionId);

        // Mock
        WikiDescriptor descriptor = new WikiDescriptor("wikiId", "alias");
        when(this.wikiDescriptorManager.getById("wikiId")).thenReturn(descriptor);

        // Test
        this.saveWikiMetaDataStep.execute(request);

        // Verify
        assertEquals("description", descriptor.getDescription());
        assertEquals("pretty name", descriptor.getPrettyName());
        verify(this.wikiDescriptorManager).saveDescriptor(descriptor);
        verify(this.wikiTemplateManager).setTemplate("wikiId", false);
        verify(this.wikiUserManager).setUserScope("wikiId", UserScope.GLOBAL_ONLY);
        verify(this.wikiUserManager).setMembershipType("wikiId", MembershipType.INVITE);
    }

    @Test
    void executeWithException() throws Exception
    {
        WikiCreationRequest request = new WikiCreationRequest();
        request.setWikiId("wikiId");

        // Mock
        WikiDescriptor descriptor = new WikiDescriptor("wikiId", "alias");
        when(this.wikiDescriptorManager.getById("wikiId")).thenReturn(descriptor);

        Exception exception = new WikiManagerException("Exception on WikiManager.");
        doThrow(exception).when(this.wikiDescriptorManager).saveDescriptor(descriptor);

        // Test and verify
        WikiCreationException caughtException = assertThrows(WikiCreationException.class,
            () -> this.saveWikiMetaDataStep.execute(request));
        assertEquals("Failed to set metadata to the wiki [wikiId].", caughtException.getMessage());
        assertEquals(exception, caughtException.getCause());
    }

    @Test
    void executeWhenSourceIsTemplate() throws Exception
    {
        WikiCreationRequest request = new WikiCreationRequest();
        request.setWikiId("wikiId");
        request.setTemplate(false);
        request.setUserScope(UserScope.LOCAL_ONLY);
        request.setMembershipType(MembershipType.OPEN);
        request.setWikiSource(WikiSource.TEMPLATE);

        // Mock
        WikiDescriptor descriptor = new WikiDescriptor("wikiId", "alias");
        when(this.wikiDescriptorManager.getById("wikiId")).thenReturn(descriptor);

        // Test
        this.saveWikiMetaDataStep.execute(request);

        // Verify
        verify(this.wikiDescriptorManager).saveDescriptor(descriptor);
        verify(this.wikiUserManager).setUserScope("wikiId", UserScope.LOCAL_ONLY);
        verify(this.wikiUserManager).setMembershipType("wikiId", MembershipType.OPEN);
    }

    @Test
    void getOrder()
    {
        assertEquals(2000, this.saveWikiMetaDataStep.getOrder());
    }
}
