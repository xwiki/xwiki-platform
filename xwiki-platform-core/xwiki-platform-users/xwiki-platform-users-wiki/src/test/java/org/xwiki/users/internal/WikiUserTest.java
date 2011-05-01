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

import java.net.URI;
import java.net.URISyntaxException;

import org.jmock.Expectations;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.test.AbstractMockingComponentTestCase;
import org.xwiki.users.User;

/**
 * Tests the invalid user flavor of {@link User}.
 * 
 * @version $Id:$
 * @since 2.5M2
 */
public class WikiUserTest extends AbstractMockingComponentTestCase
{
    private static final DocumentReference DEFAULT_USER_PROFILE = new DocumentReference("xwiki", "XWiki", "Admin");

    private EntityReferenceSerializer<String> serializer;

    private DocumentAccessBridge bridgeMock;

    private EntityReferenceResolver<EntityReference> resolver;

    User user;

    @Before
    @Override
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception
    {
        super.setUp();
        this.serializer = super.getComponentManager().lookup(EntityReferenceSerializer.class);
        this.resolver = super.getComponentManager().lookup(EntityReferenceResolver.class, "explicit/reference");
        this.bridgeMock = getMockery().mock(DocumentAccessBridge.class);
        getMockery().checking(new Expectations()
        {
            {
                allowing(WikiUserTest.this.bridgeMock).getProperty(with(WikiUserTest.DEFAULT_USER_PROFILE),
                    with(any(DocumentReference.class)), with("first_name"));
                will(returnValue("Administrator"));
                allowing(WikiUserTest.this.bridgeMock).getProperty(with(WikiUserTest.DEFAULT_USER_PROFILE),
                    with(any(DocumentReference.class)), with("last_name"));
                will(returnValue(""));
                allowing(WikiUserTest.this.bridgeMock).getProperty(with(WikiUserTest.DEFAULT_USER_PROFILE),
                    with(any(DocumentReference.class)), with("email"));
                will(returnValue("admin@xwiki.org"));
                allowing(WikiUserTest.this.bridgeMock).getDocumentURL(with(WikiUserTest.DEFAULT_USER_PROFILE),
                    with("view"), with((String) null), with((String) null));
                will(returnValue("/bin/XWiki/Admin"));
                allowing(WikiUserTest.this.bridgeMock).exists(with(DEFAULT_USER_PROFILE));
                will(returnValue(true));
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
        getMockery().checking(new Expectations()
        {
            {
                allowing(WikiUserTest.this.bridgeMock).exists(
                    with(missingUser.getProfileDocument()));
                will(returnValue(false));
            }
        });

        Assert.assertFalse(missingUser.exists());
    }

    @Test
    public void idIsReference()
    {
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
        getMockery().checking(new Expectations()
        {
            {
                allowing(WikiUserTest.this.bridgeMock).getProperty(with(specialUserProfile),
                    with(any(DocumentReference.class)), with("first_name"));
                will(returnValue(" Mr. \n"));
                allowing(WikiUserTest.this.bridgeMock).getProperty(with(specialUserProfile),
                    with(any(DocumentReference.class)), with("last_name"));
                will(returnValue(" Nobody "));
            }
        });
        Assert.assertEquals("Mr. Nobody", this.user.getName());
    }

    @Test
    public void nameIsTrimmedFirstName()
    {
        final DocumentReference specialUserProfile = new DocumentReference("xwiki", "XWiki", "aaaa");
        this.user = new WikiUser(specialUserProfile, this.serializer, this.bridgeMock, this.resolver);
        getMockery().checking(new Expectations()
        {
            {
                allowing(WikiUserTest.this.bridgeMock).getProperty(with(specialUserProfile),
                    with(any(DocumentReference.class)), with("first_name"));
                will(returnValue("Anonymous"));
                allowing(WikiUserTest.this.bridgeMock).getProperty(with(specialUserProfile),
                    with(any(DocumentReference.class)), with("last_name"));
                will(returnValue(" "));
            }
        });
        Assert.assertEquals("Anonymous", this.user.getName());
    }

    @Test
    public void nameIsTrimmedLastName()
    {
        final DocumentReference specialUserProfile = new DocumentReference("xwiki", "XWiki", "nobody");
        this.user = new WikiUser(specialUserProfile, this.serializer, this.bridgeMock, this.resolver);
        getMockery().checking(new Expectations()
        {
            {
                allowing(WikiUserTest.this.bridgeMock).getProperty(with(specialUserProfile),
                    with(any(DocumentReference.class)), with("first_name"));
                will(returnValue(""));
                allowing(WikiUserTest.this.bridgeMock).getProperty(with(specialUserProfile),
                    with(any(DocumentReference.class)), with("last_name"));
                will(returnValue(" Nobody "));
            }
        });
        Assert.assertEquals("Nobody", this.user.getName());
    }

    @Test
    public void nameFallsBackToDocumentName()
    {
        final DocumentReference specialUserProfile = new DocumentReference("xwiki", "XWiki", "ghost");
        this.user = new WikiUser(specialUserProfile, this.serializer, this.bridgeMock, this.resolver);
        getMockery().checking(new Expectations()
        {
            {
                allowing(WikiUserTest.this.bridgeMock).getProperty(with(specialUserProfile),
                    with(any(DocumentReference.class)), with("first_name"));
                will(returnValue(""));
                allowing(WikiUserTest.this.bridgeMock).getProperty(with(specialUserProfile),
                    with(any(DocumentReference.class)), with("last_name"));
                will(returnValue(""));
            }
        });
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
        getMockery().checking(new Expectations()
        {
            {
                exactly(1).of(WikiUserTest.this.bridgeMock).getProperty(with(specialUserProfile),
                    with(specialUserClass), with("motto"));
                will(returnValue("Living a lie!"));
            }
        });
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
        getMockery().checking(new Expectations()
        {
            {
                allowing(WikiUserTest.this.bridgeMock).getProperty(
                    with(userWithNameAfterInSameWiki.getProfileDocument()),
                    with(any(DocumentReference.class)), with("first_name"));
                will(returnValue("Jim"));
                allowing(WikiUserTest.this.bridgeMock).getProperty(
                    with(userWithNameAfterInSameWiki.getProfileDocument()),
                    with(any(DocumentReference.class)), with("last_name"));
                will(returnValue("James"));
            }
        });
        Assert.assertTrue(this.user.compareTo(userWithNameAfterInSameWiki) < 0);
        Assert.assertTrue(userWithNameAfterInSameWiki.compareTo(this.user) > 0);
        final User userWithNameAfterInDifferentWiki =
            new WikiUser(new DocumentReference("playground", "XWiki", "Jim"), this.serializer, this.bridgeMock,
            this.resolver);
        getMockery().checking(new Expectations()
        {
            {
                allowing(WikiUserTest.this.bridgeMock).getProperty(
                    with(userWithNameAfterInDifferentWiki.getProfileDocument()),
                    with(any(DocumentReference.class)), with("first_name"));
                will(returnValue("Jim"));
                allowing(WikiUserTest.this.bridgeMock).getProperty(
                    with(userWithNameAfterInDifferentWiki.getProfileDocument()),
                    with(any(DocumentReference.class)), with("last_name"));
                will(returnValue("James"));
            }
        });
        Assert.assertTrue(this.user.compareTo(userWithNameAfterInDifferentWiki) < 0);
        Assert.assertTrue(userWithNameAfterInDifferentWiki.compareTo(this.user) > 0);
        final User userWithNameBefore =
            new WikiUser(new DocumentReference("zone", "XWiki", "Aaron"), this.serializer, this.bridgeMock,
            this.resolver);
        getMockery().checking(new Expectations()
        {
            {
                allowing(WikiUserTest.this.bridgeMock).getProperty(
                    with(userWithNameBefore.getProfileDocument()),
                    with(any(DocumentReference.class)), with("first_name"));
                will(returnValue("Aaron"));
                allowing(WikiUserTest.this.bridgeMock).getProperty(
                    with(userWithNameBefore.getProfileDocument()),
                    with(any(DocumentReference.class)), with("last_name"));
                will(returnValue("Aronofsky"));
            }
        });
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
        getMockery().checking(new Expectations()
        {
            {
                allowing(WikiUserTest.this.bridgeMock).getProperty(with(anotherUser.getProfileDocument()),
                    with(any(DocumentReference.class)), with("first_name"));
                will(returnValue("Administrator"));
                allowing(WikiUserTest.this.bridgeMock).getProperty(with(anotherUser.getProfileDocument()),
                    with(any(DocumentReference.class)), with("last_name"));
                will(returnValue(""));
            }
        });
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
