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
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelConfiguration;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.mockito.Mockito.when;

/**
 * Tests the wiki user manager.
 * 
 * @version $Id$
 * @since 5.3M1
 */
public class WikiUserManagerTest
{
    @Rule
    public final MockitoComponentMockingRule<UserManager> mocker =
        new MockitoComponentMockingRule<UserManager>(WikiUserManager.class);

    private UserManager userManager;

    private EntityReferenceResolver<String> nameResolver;

    private EntityReferenceResolver<EntityReference> referenceResolver;

    private EntityReferenceSerializer<String> serializer;

    private ConfigurationSource configuration;

    private ModelConfiguration modelConfiguration;

    private DocumentAccessBridge bridge;

    @Before
    public void configure() throws Exception
    {
        this.nameResolver = this.mocker.getInstance(EntityReferenceResolver.TYPE_STRING, "explicit");
        this.referenceResolver = this.mocker.getInstance(EntityReferenceResolver.TYPE_REFERENCE, "explicit");
        this.serializer = this.mocker.getInstance(EntityReferenceSerializer.TYPE_STRING);
        this.configuration = this.mocker.getInstance(ConfigurationSource.class, "xwikiproperties");
        this.modelConfiguration = this.mocker.getInstance(ModelConfiguration.class);
        this.bridge = this.mocker.getInstance(DocumentAccessBridge.class);

        when(this.configuration.getProperty("users.defaultUserSpace", "XWiki")).thenReturn("XWiki");
        when(WikiUserManagerTest.this.modelConfiguration.getDefaultReferenceValue(EntityType.WIKI)).thenReturn("xwiki");

        this.userManager = this.mocker.getComponentUnderTest();
    }

    @Test
    public void getUserFromLocalReference() throws Exception
    {
        setupMocks("XWiki.Admin", "xwiki");
        User u = this.userManager.getUser("XWiki.Admin");
        Assert.assertEquals("xwiki:XWiki.Admin", u.getId());
        Assert.assertTrue(u instanceof WikiUser);
    }

    @Test
    public void getUserFromFullReference() throws Exception
    {
        setupMocks("playground:SouthPark.Timmy", "SouthPark", "playground", true);
        User u = this.userManager.getUser("playground:SouthPark.Timmy");
        Assert.assertEquals("playground:SouthPark.Timmy", u.getId());
        Assert.assertTrue(u instanceof WikiUser);
    }

    @Test
    public void differentSettingsAndUserInLocalWiki() throws Exception
    {
        setupMocks("local");
        User u = this.userManager.getUser("Admin");
        Assert.assertEquals("local:XWiki.Admin", u.getId());
        Assert.assertTrue(u instanceof WikiUser);
    }

    @Test
    public void differentSettingsAndUserInUsersWiki() throws Exception
    {
        setupMocks("users");
        User u = this.userManager.getUser("Admin");
        Assert.assertEquals("users:XWiki.Admin", u.getId());
        Assert.assertTrue(u instanceof WikiUser);
    }

    @Test
    public void differentSettingsAndUserInGlobalWiki() throws Exception
    {
        setupMocks("xwiki");
        User u = this.userManager.getUser("Admin");
        Assert.assertEquals("xwiki:XWiki.Admin", u.getId());
        Assert.assertTrue(u instanceof WikiUser);
    }

    @Test
    public void differentSettingsAndMissingUser() throws Exception
    {
        setupMocks("Admin", "XWiki", "users", false);
        User u = this.userManager.getUser("Admin");
        Assert.assertNull(u);
    }

    @Test
    public void forceWithExistingUserInLocalWiki() throws Exception
    {
        setupMocks("local");
        User u = this.userManager.getUser("Admin", true);
        Assert.assertEquals("local:XWiki.Admin", u.getId());
        Assert.assertTrue(u.exists());
    }

    @Test
    public void forceWithExistingUserInUsersWiki() throws Exception
    {
        setupMocks("users");
        User u = this.userManager.getUser("Admin", true);
        Assert.assertEquals("users:XWiki.Admin", u.getId());
        Assert.assertTrue(u.exists());
    }

    @Test
    public void forceWithExistingUserInGlobalWiki() throws Exception
    {
        setupMocks("xwiki");
        User u = this.userManager.getUser("Admin", true);
        Assert.assertEquals("xwiki:XWiki.Admin", u.getId());
        Assert.assertTrue(u.exists());
    }

    @Test
    public void forceWithMissingUser() throws Exception
    {
        setupMocks("Admin", "XWiki", "users", false);
        User u = this.userManager.getUser("Admin", true);
        Assert.assertEquals("users:XWiki.Admin", u.getId());
        Assert.assertFalse(u.exists());
    }

    @Test
    public void nullIdentifierReturnsNull() throws Exception
    {
        User u = this.userManager.getUser(null);
        Assert.assertNull(u);
    }

    @Test
    public void emptyIdentifierReturnsNull() throws Exception
    {
        User u = this.userManager.getUser("");
        Assert.assertNull(u);
    }

    @Test
    public void blankIdentifierReturnsNull() throws Exception
    {
        User u = this.userManager.getUser("\n \t");
        Assert.assertNull(u);
    }

    @Test
    public void blankIdentifierAndForceReturnsNull() throws Exception
    {
        User u = this.userManager.getUser("\n \t", true);
        Assert.assertNull(u);
    }

    private void setupMocks(final String targetUserWiki)
    {
        setupMocks("Admin", "XWiki", targetUserWiki, true);
    }

    private void setupMocks(final String passedIdentifier, final String targetUserWiki)
    {
        setupMocks(passedIdentifier, "XWiki", targetUserWiki, true);
    }

    private void setupMocks(final String passedIdentifier, final String targetSpace, final String targetUserWiki,
        final boolean existing)
    {
        final DocumentReference targetUser = new DocumentReference(targetUserWiki, "XWiki", "Admin");
        when(this.configuration.getProperty("users.defaultWiki", "local")).thenReturn("users");
        when(this.bridge.getCurrentDocumentReference()).thenReturn(new DocumentReference("local", "Main", "WebHome"));

        when(this.bridge.exists(targetUser)).thenReturn(existing);
        when(this.bridge.exists(new DocumentReference("xwiki", targetSpace, "Admin"))).thenReturn(false);
        when(this.bridge.exists(new DocumentReference("users", targetSpace, "Admin"))).thenReturn(false);
        when(this.bridge.exists(new DocumentReference("local", targetSpace, "Admin"))).thenReturn(false);

        when(this.referenceResolver.resolve(
            new EntityReference("XWikiUsers", EntityType.DOCUMENT, new EntityReference("XWiki",
                EntityType.SPACE)), EntityType.DOCUMENT, targetUser))
            .thenReturn(new DocumentReference(targetUserWiki, "XWiki", "XWikiUsers"));

        when(this.serializer.serialize(targetUser, new Object[0])).thenReturn(targetUserWiki + ":" + targetSpace + "."
            + StringUtils.defaultIfEmpty(StringUtils.substringAfterLast(passedIdentifier, "."),
                passedIdentifier));

        if (passedIdentifier.startsWith(targetUserWiki + ":")) {
            when(this.nameResolver.resolve(passedIdentifier, EntityType.DOCUMENT,
                new EntityReference("XWiki", EntityType.SPACE, new WikiReference("local")))).thenReturn(targetUser);
        } else {
            when(this.nameResolver.resolve(passedIdentifier, EntityType.DOCUMENT,
                new EntityReference("XWiki", EntityType.SPACE, new WikiReference("xwiki"))))
                .thenReturn(new DocumentReference("xwiki", targetSpace, "Admin"));
            when(this.nameResolver.resolve(passedIdentifier, EntityType.DOCUMENT,
                new EntityReference("XWiki", EntityType.SPACE, new WikiReference("users"))))
                .thenReturn(new DocumentReference("users", targetSpace, "Admin"));
            when(this.nameResolver.resolve(passedIdentifier, EntityType.DOCUMENT,
                new EntityReference("XWiki", EntityType.SPACE, new WikiReference("local"))))
                .thenReturn(new DocumentReference("local", targetSpace, "Admin"));
        }
    }
}
