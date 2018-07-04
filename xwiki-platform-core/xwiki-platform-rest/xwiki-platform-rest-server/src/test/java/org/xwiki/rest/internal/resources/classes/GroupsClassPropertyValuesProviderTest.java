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
package org.xwiki.rest.internal.resources.classes;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.ClassPropertyReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rest.model.jaxb.PropertyValues;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.user.UserScope;
import org.xwiki.wiki.user.WikiUserManager;

import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.GroupsClass;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link GroupsClassPropertyValuesProvider}.
 *
 * @version $Id$
 */
@ComponentTest
public class GroupsClassPropertyValuesProviderTest extends AbstractListClassPropertyValuesProviderTest
{
    @InjectMockComponents
    private GroupsClassPropertyValuesProvider provider;

    private WikiUserManager wikiUserManager;

    private ClassPropertyReference propertyReference = new ClassPropertyReference("band", this.classReference);

    @BeforeEach
    public void configure() throws Exception
    {
        super.configure();

        addProperty(this.propertyReference.getName(), new GroupsClass(), true);
        when(this.xcontext.getWiki().getSkinFile("icons/xwiki/noavatargroup.png", true, this.xcontext))
            .thenReturn("url/to/noavatar.png");

        this.wikiUserManager = this.componentManager.getInstance(WikiUserManager.class);
    }

    @Test
    public void getValuesLocal() throws Exception
    {
        when(this.wikiUserManager.getUserScope(this.classReference.getWikiReference().getName()))
            .thenReturn(UserScope.LOCAL_ONLY);

        DocumentReference devsReference = new DocumentReference("wiki", "Groups", "Devs");
        XWikiDocument devsProfile = mock(XWikiDocument.class, "devs");
        when(this.xcontext.getWiki().getDocument(devsReference, this.xcontext)).thenReturn(devsProfile);
        when(devsProfile.getRenderedTitle(Syntax.PLAIN_1_0, this.xcontext)).thenReturn("Developers");

        DocumentReference adminsReference = new DocumentReference("wiki", "Groups", "Admins");
        XWikiDocument adminsProfile = mock(XWikiDocument.class, "admins");
        XWikiAttachment notAnImageAttachment = mock(XWikiAttachment.class, "noAnImage");
        XWikiAttachment imageAttachment = mock(XWikiAttachment.class, "image");
        AttachmentReference imageAttachmentReference = new AttachmentReference("admins.png", adminsReference);
        when(this.xcontext.getWiki().getDocument(adminsReference, this.xcontext)).thenReturn(adminsProfile);
        when(adminsProfile.getRenderedTitle(Syntax.PLAIN_1_0, this.xcontext)).thenReturn("Administrators");
        when(adminsProfile.getAttachmentList()).thenReturn(Arrays.asList(notAnImageAttachment, imageAttachment));
        when(imageAttachment.isImage(this.xcontext)).thenReturn(true);
        when(imageAttachment.getReference()).thenReturn(imageAttachmentReference);
        when(this.xcontext.getWiki().getURL(imageAttachmentReference, "download",
            "width=30&height=30&keepAspectRatio=true", null, this.xcontext)).thenReturn("url/to/admins/image");

        when(this.allowedValuesQuery.execute()).thenReturn(Arrays.asList(devsReference, adminsReference));

        PropertyValues values = this.provider.getValues(this.propertyReference, 5, "foo");

        assertEquals(2, values.getPropertyValues().size());

        assertEquals("Developers", values.getPropertyValues().get(0).getMetaData().get("label"));
        assertEquals("url/to/noavatar.png", values.getPropertyValues().get(0).getMetaData().get("icon"));

        assertEquals("Administrators", values.getPropertyValues().get(1).getMetaData().get("label"));
        assertEquals("url/to/admins/image", values.getPropertyValues().get(1).getMetaData().get("icon"));
    }

    @Test
    public void getValuesLocalAndGlobal() throws Exception
    {
        // We can have local groups.
        when(this.wikiUserManager.getUserScope(this.classReference.getWikiReference().getName()))
            .thenReturn(UserScope.GLOBAL_ONLY);

        WikiDescriptorManager wikiDescriptorManager = this.componentManager.getInstance(WikiDescriptorManager.class);
        when(wikiDescriptorManager.getMainWikiId()).thenReturn("chess");

        this.provider.getValues(this.propertyReference, 5, "foo");

        verify(this.allowedValuesQuery).setWiki("chess");
        verify(this.allowedValuesQuery, times(2)).execute();
    }
}
