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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.ClassPropertyReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.QueryFilter;
import org.xwiki.rest.model.jaxb.PropertyValues;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserConfiguration;
import org.xwiki.user.UserProperties;
import org.xwiki.user.UserPropertiesResolver;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.user.UserScope;
import org.xwiki.wiki.user.WikiUserManager;

import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.UsersClass;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link UsersClassPropertyValuesProvider}.
 *
 * @version $Id$
 * @since 9.8
 */
@ComponentTest
public class UsersClassPropertyValuesProviderTest extends AbstractListClassPropertyValuesProviderTest
{
    @InjectMockComponents
    private UsersClassPropertyValuesProvider provider;

    @MockComponent
    private WikiUserManager wikiUserManager;

    private ClassPropertyReference propertyReference = new ClassPropertyReference("owner", this.classReference);

    @MockComponent
    private UserConfiguration userConfiguration;

    @MockComponent
    @Named("document")
    private UserReferenceResolver<DocumentReference> userReferenceResolver;

    @MockComponent
    private UserPropertiesResolver userPropertiesResolver;

    @BeforeEach
    public void configure() throws Exception
    {
        super.configure();

        addProperty(this.propertyReference.getName(), new UsersClass(), true);
        when(this.xcontext.getWiki().getSkinFile("icons/xwiki/noavatar.png", true, this.xcontext))
            .thenReturn("url/to/noavatar.png");
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void getValuesLocal(boolean hasAccess) throws Exception
    {
        when(this.authorizationManager.hasAccess(any(), any())).thenReturn(hasAccess);

        when(this.wikiUserManager.getUserScope(this.classReference.getWikiReference().getName()))
            .thenReturn(UserScope.LOCAL_ONLY);

        DocumentReference aliceReference = new DocumentReference("wiki", "Users", "Alice");
        when(this.allowedValuesQuery.execute())
            .thenReturn(Collections.singletonList(new Object[] { aliceReference, " Alice One " }));
        when(this.xcontext.getWiki().getDocument(aliceReference, this.xcontext))
            .thenReturn(mock(XWikiDocument.class, "alice"));

        QueryFilter documentFilter = this.componentManager.getInstance(QueryFilter.class, "document");
        QueryFilter viewableFilter = this.componentManager.getInstance(QueryFilter.class, "viewable");
        List<QueryFilter> filters = mock(List.class);
        DocumentReference bobReference = new DocumentReference("wiki", "Users", "Bob");
        when(this.usedValuesQuery.getFilters()).thenReturn(filters);
        when(this.usedValuesQuery.execute())
            .thenReturn(Arrays.asList(new Object[] { bobReference, 17L }, new Object[] { aliceReference, 3L }));

        EntityReferenceSerializer<String> compactSerializer =
            this.componentManager.getInstance(EntityReferenceSerializer.TYPE_STRING, "compact");
        when(compactSerializer.serialize(aliceReference, this.classReference.getWikiReference()))
            .thenReturn("Users.Alice");
        when(compactSerializer.serialize(bobReference, this.classReference.getWikiReference())).thenReturn("Users.Bob");

        when(this.xcontext.getWiki().getPlainUserName(bobReference, this.xcontext)).thenReturn("Bob the Great");

        when(this.xcontext.getWiki().getURL(aliceReference, this.xcontext)).thenReturn("url/to/alice");
        when(this.xcontext.getWiki().getURL(bobReference, this.xcontext)).thenReturn("url/to/bob");

        XWikiDocument bobProfile = mock(XWikiDocument.class);
        XWikiAttachment bobAvatar = mock(XWikiAttachment.class);
        AttachmentReference bobAvatarReference = new AttachmentReference("somePhoto.png", bobReference);
        when(this.xcontext.getWiki().getDocument(bobReference, this.xcontext)).thenReturn(bobProfile);
        when(bobProfile.getStringValue("avatar")).thenReturn(bobAvatarReference.getName());
        when(bobProfile.getAttachment("somePhoto.png")).thenReturn(bobAvatar);
        when(bobAvatar.isImage(this.xcontext)).thenReturn(true);
        when(bobAvatar.getReference()).thenReturn(bobAvatarReference);
        when(this.xcontext.getWiki().getURL(bobAvatarReference, "download", "width=30&height=30&keepAspectRatio=true",
            null, this.xcontext)).thenReturn("url/to/bob/avatar");

        PropertyValues values = this.provider.getValues(this.propertyReference, 5, "foo");

        assertEquals(2, values.getPropertyValues().size());

        assertEquals("Users.Alice", values.getPropertyValues().get(0).getValue());
        assertEquals("Alice One", values.getPropertyValues().get(0).getMetaData().get("label"));
        assertEquals(3L, values.getPropertyValues().get(0).getMetaData().get("count"));
        assertTrue(values.getPropertyValues().get(0).getMetaData().get("icon") instanceof Map);
        assertEquals("url/to/alice", values.getPropertyValues().get(0).getMetaData().get("url"));

        assertEquals("Users.Bob", values.getPropertyValues().get(1).getValue());
        assertEquals("Bob the Great", values.getPropertyValues().get(1).getMetaData().get("label"));
        assertEquals(17L, values.getPropertyValues().get(1).getMetaData().get("count"));
        assertTrue(values.getPropertyValues().get(1).getMetaData().get("icon") instanceof Map);
        Map icon = (Map) values.getPropertyValues().get(1).getMetaData().get("icon");
        if (hasAccess) {
            assertEquals("url/to/bob/avatar", icon.get("url"));
            assertEquals("IMAGE", icon.get("iconSetType"));
        } else {
            assertEquals("user", mockingDetails(icon).getMockCreationSettings().getMockName().toString());
        }
        assertEquals("url/to/bob", values.getPropertyValues().get(1).getMetaData().get("url"));

        verify(this.allowedValuesQuery, never()).setWiki(any(String.class));
        verify(this.allowedValuesQuery, times(1)).execute();

        verify(filters).clear();
        verify(this.usedValuesQuery).addFilter(documentFilter);
        verify(this.usedValuesQuery).addFilter(viewableFilter);
    }

    @Test
    public void getValuesGlobal() throws Exception
    {
        when(this.wikiUserManager.getUserScope(this.classReference.getWikiReference().getName()))
            .thenReturn(UserScope.GLOBAL_ONLY);

        WikiDescriptorManager wikiDescriptorManager = this.componentManager.getInstance(WikiDescriptorManager.class);
        when(wikiDescriptorManager.getMainWikiId()).thenReturn("math");

        this.provider.getValues(this.propertyReference, 5, "foo");

        verify(this.allowedValuesQuery).setWiki("math");
        verify(this.allowedValuesQuery, times(1)).execute();
    }

    @Test
    public void getValuesLocalAndGlobal() throws Exception
    {
        when(this.wikiUserManager.getUserScope(this.classReference.getWikiReference().getName()))
            .thenReturn(UserScope.LOCAL_AND_GLOBAL);

        WikiDescriptorManager wikiDescriptorManager = this.componentManager.getInstance(WikiDescriptorManager.class);
        when(wikiDescriptorManager.getMainWikiId()).thenReturn("chess");

        DocumentReference aliceReference = new DocumentReference("wiki", "Users", "Alice");
        when(this.xcontext.getWiki().getDocument(aliceReference, this.xcontext))
            .thenReturn(mock(XWikiDocument.class, "alice"));
        DocumentReference bobReference = new DocumentReference("chess", "Users", "Bob");
        when(this.xcontext.getWiki().getDocument(bobReference, this.xcontext))
            .thenReturn(mock(XWikiDocument.class, "bob"));
        when(this.allowedValuesQuery.execute()).thenReturn(Collections.singletonList(bobReference),
            Collections.singletonList(aliceReference));

        when(this.xcontext.getWiki().getPlainUserName(aliceReference, this.xcontext)).thenReturn("Alice White");
        when(this.xcontext.getWiki().getPlainUserName(bobReference, this.xcontext)).thenReturn("Bob Black");

        when(this.userConfiguration.getUserQualifierProperty()).thenReturn("address");
        UserReference aliceUserReference = mock(UserReference.class, "alice");
        when(this.userReferenceResolver.resolve(aliceReference)).thenReturn(aliceUserReference);
        UserReference bobUserReference = mock(UserReference.class, "bob");
        when(this.userReferenceResolver.resolve(bobReference)).thenReturn(bobUserReference);
        UserProperties aliceProperties = mock(UserProperties.class, "alice");
        when(this.userPropertiesResolver.resolve(aliceUserReference)).thenReturn(aliceProperties);
        UserProperties bobProperties = mock(UserProperties.class, "bob");
        when(this.userPropertiesResolver.resolve(bobUserReference)).thenReturn(bobProperties);
        when(aliceProperties.getProperty("address")).thenReturn("Paris, France");
        when(bobProperties.getProperty("address")).thenReturn("Iasi, Romania");

        PropertyValues values = this.provider.getValues(this.propertyReference, 5, "foo");

        assertEquals(2, values.getPropertyValues().size());
        assertEquals("Alice White", values.getPropertyValues().get(0).getMetaData().get("label"));
        assertEquals("Paris, France", values.getPropertyValues().get(0).getMetaData().get("hint"));
        assertEquals("Bob Black", values.getPropertyValues().get(1).getMetaData().get("label"));
        assertEquals("Iasi, Romania", values.getPropertyValues().get(1).getMetaData().get("hint"));

        verify(this.allowedValuesQuery).setWiki("chess");
        verify(this.allowedValuesQuery, times(2)).execute();
    }
}
