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
package org.xwiki.users.internal;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.users.User;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the invalid user flavor of {@link User}.
 * 
 * @version $Id$
 * @since 5.3M1
 */
public class WikiUserTest
{
    private static final DocumentReference DEFAULT_USER_PROFILE = new DocumentReference("xwiki", "XWiki", "Admin");

    private EntityReferenceSerializer<String> serializer;

    private DocumentAccessBridge bridgeMock;

    private EntityReferenceResolver<EntityReference> resolver;

    User user;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception
    {
        this.serializer = mock(EntityReferenceSerializer.class);
        this.resolver = mock(EntityReferenceResolver.class);
        this.bridgeMock = mock(DocumentAccessBridge.class);
        when(this.bridgeMock.getProperty(same(WikiUserTest.DEFAULT_USER_PROFILE),
            any(DocumentReference.class), same("first_name"))).thenReturn("Administrator");
        when(this.bridgeMock.getProperty(same(WikiUserTest.DEFAULT_USER_PROFILE),
            any(DocumentReference.class), same("last_name"))).thenReturn("");
        when(this.bridgeMock.getProperty(same(WikiUserTest.DEFAULT_USER_PROFILE),
            any(DocumentReference.class), same("email"))).thenReturn("admin@xwiki.org");
        when(this.bridgeMock.getDocumentURL(WikiUserTest.DEFAULT_USER_PROFILE,
            "view", (String) null, (String) null)).thenReturn("/bin/XWiki/Admin");
        when(this.bridgeMock.exists(DEFAULT_USER_PROFILE)).thenReturn(true);
        when(this.resolver.resolve(any(EntityReference.class), any(EntityType.class), any(DocumentReference.class)))
            .thenAnswer(
                new Answer<EntityReference>()
                {
                    @Override
                    public EntityReference answer(InvocationOnMock invocation) throws Throwable
                    {
                        EntityReference toResolve = (EntityReference) invocation.getArguments()[0];
                        DocumentReference reference = (DocumentReference) invocation.getArguments()[2];
                        return new DocumentReference(toResolve.getName(),
                            new SpaceReference(toResolve.getParent().getName(), reference.getRoot()));
                    }
                });

        this.user = new WikiUser(DEFAULT_USER_PROFILE, this.serializer, this.bridgeMock, this.resolver);
    }

    @Test
    public void existsWithExistingUser()
    {
        Assert.assertTrue(this.user.exists());
    }

    @Test
    public void existsWithMissingUser()
    {
        final User missingUser =
            new WikiUser(new DocumentReference("xwiki", "XWiki", "XWikiGuest"), this.serializer, this.bridgeMock,
                this.resolver);
        when(this.bridgeMock.exists(missingUser.getProfileDocument())).thenReturn(false);
        Assert.assertFalse(missingUser.exists());
    }

    @Test
    public void idIsReference()
    {
        when(this.serializer.serialize(DEFAULT_USER_PROFILE)).thenReturn("xwiki:XWiki.Admin");
        Assert.assertEquals("xwiki:XWiki.Admin", this.user.getId());
    }

    @Test
    public void usernameIsDocumentName()
    {
        Assert.assertEquals("Admin", this.user.getUsername());
    }

    @Test
    public void nameIsTrimmedFirstAndLastName()
    {
        final DocumentReference specialUserProfile = new DocumentReference("xwiki", "XWiki", "nobody");
        this.user = new WikiUser(specialUserProfile, this.serializer, this.bridgeMock, this.resolver);
        when(this.bridgeMock.getProperty(same(specialUserProfile), any(DocumentReference.class), same("first_name")))
            .thenReturn(" Mr. \n");
        when(this.bridgeMock.getProperty(same(specialUserProfile), any(DocumentReference.class), same("last_name")))
            .thenReturn(" Nobody ");
        Assert.assertEquals("Mr. Nobody", this.user.getName());
    }

    @Test
    public void nameIsTrimmedFirstName()
    {
        final DocumentReference specialUserProfile = new DocumentReference("xwiki", "XWiki", "aaaa");
        this.user = new WikiUser(specialUserProfile, this.serializer, this.bridgeMock, this.resolver);
        when(this.bridgeMock.getProperty(same(specialUserProfile), any(DocumentReference.class), same("first_name")))
            .thenReturn("Anonymous");
        when(this.bridgeMock.getProperty(same(specialUserProfile), any(DocumentReference.class), same("last_name")))
            .thenReturn(" ");
        Assert.assertEquals("Anonymous", this.user.getName());
    }

    @Test
    public void nameIsTrimmedLastName()
    {
        final DocumentReference specialUserProfile = new DocumentReference("xwiki", "XWiki", "nobody");
        this.user = new WikiUser(specialUserProfile, this.serializer, this.bridgeMock, this.resolver);
        when(this.bridgeMock.getProperty(same(specialUserProfile), any(DocumentReference.class), same("first_name")))
            .thenReturn("");
        when(this.bridgeMock.getProperty(same(specialUserProfile), any(DocumentReference.class), same("last_name")))
            .thenReturn(" Nobody ");
        Assert.assertEquals("Nobody", this.user.getName());
    }

    @Test
    public void nameFallsBackToDocumentName()
    {
        final DocumentReference specialUserProfile = new DocumentReference("xwiki", "XWiki", "ghost");
        this.user = new WikiUser(specialUserProfile, this.serializer, this.bridgeMock, this.resolver);
        when(this.bridgeMock.getProperty(same(specialUserProfile), any(DocumentReference.class), same("first_name")))
            .thenReturn("");
        when(this.bridgeMock.getProperty(same(specialUserProfile), any(DocumentReference.class), same("last_name")))
            .thenReturn("");
        Assert.assertEquals("ghost", this.user.getName());
    }

    @Test
    public void toStringIsName()
    {
        Assert.assertEquals("Administrator", this.user.toString());
    }

    @Test
    public void rightProfileDocumentIsReturned()
    {
        Assert.assertEquals(DEFAULT_USER_PROFILE, this.user.getProfileDocument());
    }

    @Test
    public void rightProfileURIIsReturned() throws URISyntaxException
    {
        Assert.assertEquals(new URI("/bin/XWiki/Admin"), this.user.getProfileURI());
    }

    @Test
    public void attributeIsReturned()
    {
        Assert.assertEquals("admin@xwiki.org", this.user.getAttribute("email"));
    }

    @Test
    public void attributeWithNonLocalProfileUsesRightClass()
    {
        final DocumentReference specialUserProfile = new DocumentReference("playground", "SouthPark", "timmy");
        final DocumentReference specialUserClass = new DocumentReference("playground", "XWiki", "XWikiUsers");
        this.user = new WikiUser(specialUserProfile, this.serializer, this.bridgeMock, this.resolver);
        when(this.bridgeMock.getProperty(specialUserProfile, specialUserClass, "motto")).thenReturn("Living a lie!");
        Assert.assertEquals("Living a lie!", this.user.getAttribute("motto"));
    }

    @Test
    public void nullReferenceIsOK()
    {
        this.user = new WikiUser(null, this.serializer, this.bridgeMock, this.resolver);
        Assert.assertFalse(this.user.exists());
        Assert.assertEquals("", this.user.getId());
        Assert.assertEquals("", this.user.getUsername());
        Assert.assertEquals("", this.user.getName());
        Assert.assertNull(this.user.getProfileDocument());
        Assert.assertNull(this.user.getProfileURI());
        Assert.assertNull(this.user.getAttribute("email"));
        Assert.assertEquals("", this.user.toString());
    }

    @Test
    public void compareOrdersByName()
    {
        final User userWithNameAfterInSameWiki =
            new WikiUser(new DocumentReference("xwiki", "XWiki", "Jim"), this.serializer, this.bridgeMock,
                this.resolver);
        when(this.bridgeMock.getProperty(same(userWithNameAfterInSameWiki.getProfileDocument()),
            any(DocumentReference.class), same("first_name"))).thenReturn("Jim");
        when(this.bridgeMock.getProperty(same(userWithNameAfterInSameWiki.getProfileDocument()),
            any(DocumentReference.class), same("last_name"))).thenReturn("James");
        Assert.assertTrue(this.user.compareTo(userWithNameAfterInSameWiki) < 0);
        Assert.assertTrue(userWithNameAfterInSameWiki.compareTo(this.user) > 0);

        final User userWithNameAfterInDifferentWiki =
            new WikiUser(new DocumentReference("playground", "XWiki", "Jim"), this.serializer, this.bridgeMock,
                this.resolver);
        when(this.bridgeMock.getProperty(same(userWithNameAfterInDifferentWiki.getProfileDocument()),
            any(DocumentReference.class), same("first_name"))).thenReturn("Jim");
        when(this.bridgeMock.getProperty(same(userWithNameAfterInDifferentWiki.getProfileDocument()),
            any(DocumentReference.class), same("last_name"))).thenReturn("James");
        Assert.assertTrue(this.user.compareTo(userWithNameAfterInDifferentWiki) < 0);
        Assert.assertTrue(userWithNameAfterInDifferentWiki.compareTo(this.user) > 0);

        final User userWithNameBefore =
            new WikiUser(new DocumentReference("zone", "XWiki", "Aaron"), this.serializer, this.bridgeMock,
                this.resolver);
        when(this.bridgeMock.getProperty(same(userWithNameBefore.getProfileDocument()),
            any(DocumentReference.class), same("first_name"))).thenReturn("Aaron");
        when(this.bridgeMock.getProperty(same(userWithNameBefore.getProfileDocument()),
            any(DocumentReference.class), same("last_name"))).thenReturn("Aronofsky");
        Assert.assertTrue(this.user.compareTo(userWithNameBefore) > 0);
        Assert.assertTrue(userWithNameBefore.compareTo(this.user) < 0);
    }

    @Test
    public void compareWithSameName()
    {
        Assert.assertTrue(this.user.compareTo(this.user) == 0);
        final User anotherUser =
            new WikiUser(new DocumentReference("playground", "XWiki", "Admin"), this.serializer, this.bridgeMock,
                this.resolver);

        when(this.bridgeMock.getProperty(same(anotherUser.getProfileDocument()),
            any(DocumentReference.class), same("first_name"))).thenReturn("Administrator");
        when(this.bridgeMock.getProperty(same(anotherUser.getProfileDocument()),
            any(DocumentReference.class), same("last_name"))).thenReturn("");

        Assert.assertTrue(this.user.compareTo(anotherUser) == 0);
        Assert.assertTrue(anotherUser.compareTo(this.user) == 0);
    }

    @Test(expected = NullPointerException.class)
    public void compareToNullThrowsNPE()
    {
        this.user.compareTo(null);
        Assert.fail("NPE when comparing with null not thrown");
    }
}
