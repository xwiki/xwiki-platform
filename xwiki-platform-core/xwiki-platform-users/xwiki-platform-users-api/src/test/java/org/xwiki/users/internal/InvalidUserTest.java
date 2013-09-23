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

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.users.User;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the invalid user flavor of {@link User}.
 * 
 * @version $Id$
 * @since 3.1M2
 */
public class InvalidUserTest
{
    private EntityReferenceSerializer<String> serializer;

    private User user;

    private DocumentReference profile = new DocumentReference("xwiki", "XWiki", "Admin");

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception
    {
        this.serializer = mock(EntityReferenceSerializer.class);
        this.user = new InvalidUser(this.profile, this.serializer);
    }

    @Test
    public void userDoesNotExist()
    {
        Assert.assertFalse(this.user.exists());
    }

    @Test
    public void idIsReference()
    {
        when(this.serializer.serialize(this.profile)).thenReturn("xwiki:XWiki.Admin");
        Assert.assertEquals("xwiki:XWiki.Admin", this.user.getId());
    }

    @Test
    public void usernameIsDocumentName()
    {
        Assert.assertEquals("Admin", this.user.getUsername());
    }

    @Test
    public void nameIsDocumentName()
    {
        Assert.assertEquals("Admin", this.user.getName());
    }

    @Test
    public void toStringIsDocumentName()
    {
        Assert.assertEquals("Admin", this.user.toString());
    }

    @Test
    public void nullProfileDocumentIsReturned()
    {
        Assert.assertNull(this.user.getProfileDocument());
    }

    @Test
    public void nullProfileURIIsReturned()
    {
        Assert.assertNull(this.user.getProfileURI());
    }

    @Test
    public void nullAttributeIsReturned()
    {
        Assert.assertNull(this.user.getAttribute("email"));
    }

    @Test
    public void nullReferenceIsOK()
    {
        this.user = new InvalidUser(null, this.serializer);
        Assert.assertNotNull(this.user);
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
        User anotherUser = new InvalidUser(new DocumentReference("xwiki", "XWiki", "Jim"), this.serializer);
        Assert.assertTrue(this.user.compareTo(anotherUser) < 0);
        Assert.assertTrue(anotherUser.compareTo(this.user) > 0);

        anotherUser = new InvalidUser(new DocumentReference("playground", "XWiki", "Jim"), this.serializer);
        Assert.assertTrue(this.user.compareTo(anotherUser) < 0);
        Assert.assertTrue(anotherUser.compareTo(this.user) > 0);

        anotherUser = new InvalidUser(new DocumentReference("zone", "XWiki", "Aaron"), this.serializer);
        Assert.assertTrue(this.user.compareTo(anotherUser) > 0);
        Assert.assertTrue(anotherUser.compareTo(this.user) < 0);

        anotherUser = new InvalidUser(null, this.serializer);
        Assert.assertTrue(this.user.compareTo(anotherUser) > 0);
        Assert.assertTrue(anotherUser.compareTo(this.user) < 0);
    }

    @Test
    public void compareWithSameName()
    {
        Assert.assertTrue(this.user.compareTo(this.user) == 0);
        User anotherUser = new InvalidUser(new DocumentReference("playground", "XWiki", "Admin"), this.serializer);
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
