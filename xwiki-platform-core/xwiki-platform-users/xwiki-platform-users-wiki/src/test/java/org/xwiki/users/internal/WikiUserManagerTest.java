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

import org.apache.commons.lang.StringUtils;
import org.jmock.Expectations;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelConfiguration;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.AbstractMockingComponentTestCase;
import org.xwiki.test.annotation.MockingRequirement;
import org.xwiki.users.User;

/**
 * Tests the wiki user manager.
 * 
 * @version $Id:$
 * @since 3.1M2
 */
public class WikiUserManagerTest extends AbstractMockingComponentTestCase
{
    @MockingRequirement
    private WikiUserManager userManager;

    private EntityReferenceResolver<String> nameResolver;

    private EntityReferenceResolver<EntityReference> referenceResolver;

    private EntityReferenceSerializer<String> serializer;

    private ConfigurationSource configuration;

    private ModelConfiguration modelConfiguration;

    private DocumentAccessBridge bridge;

    @Before
    @Override
    public void setUp() throws Exception
    {
        super.setUp();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void configure() throws Exception
    {
        this.nameResolver = getComponentManager().lookup(EntityReferenceResolver.class, "explicit");
        this.referenceResolver = getComponentManager().lookup(EntityReferenceResolver.class, "explicit/reference");
        this.serializer = getComponentManager().lookup(EntityReferenceSerializer.class);
        this.configuration = getComponentManager().lookup(ConfigurationSource.class, "xwikiproperties");
        this.modelConfiguration = getComponentManager().lookup(ModelConfiguration.class);
        this.bridge = getComponentManager().lookup(DocumentAccessBridge.class);

        getMockery().checking(new Expectations()
        {
            {
                allowing(WikiUserManagerTest.this.configuration).getProperty("users.defaultUserSpace", "XWiki");
                will(returnValue("XWiki"));

                allowing(WikiUserManagerTest.this.modelConfiguration).getDefaultReferenceValue(EntityType.WIKI);
                will(returnValue("xwiki"));
            }
        });
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
        getMockery().checking(new Expectations()
        {
            {
                allowing(WikiUserManagerTest.this.configuration).getProperty("users.defaultWiki", "local");
                will(returnValue("users"));
                allowing(WikiUserManagerTest.this.bridge).getCurrentDocumentReference();
                will(returnValue(new DocumentReference("local", "Main", "WebHome")));

                allowing(WikiUserManagerTest.this.bridge).exists(targetUser);
                will(returnValue(existing));
                allowing(WikiUserManagerTest.this.bridge).exists(new DocumentReference("xwiki", targetSpace, "Admin"));
                will(returnValue(false));
                allowing(WikiUserManagerTest.this.bridge).exists(new DocumentReference("users", targetSpace, "Admin"));
                will(returnValue(false));
                allowing(WikiUserManagerTest.this.bridge).exists(new DocumentReference("local", targetSpace, "Admin"));
                will(returnValue(false));

                allowing(WikiUserManagerTest.this.referenceResolver).resolve(
                    new EntityReference("XWikiUsers", EntityType.DOCUMENT, new EntityReference("XWiki",
                    EntityType.SPACE)), EntityType.DOCUMENT, targetUser);
                will(returnValue(new DocumentReference(targetUserWiki, "XWiki", "XWikiUsers")));

                allowing(WikiUserManagerTest.this.serializer).serialize(with(targetUser), with(new Object[0]));
                will(returnValue(targetUserWiki + ":" + targetSpace + "."
                    + StringUtils.defaultIfEmpty(StringUtils.substringAfterLast(passedIdentifier, "."),
                    passedIdentifier)));

                if (passedIdentifier.startsWith(targetUserWiki + ":")) {
                    exactly(1).of(WikiUserManagerTest.this.nameResolver).resolve(passedIdentifier, EntityType.DOCUMENT,
                        new EntityReference("XWiki", EntityType.SPACE, new WikiReference("local")));
                    will(returnValue(targetUser));
                } else {
                    allowing(WikiUserManagerTest.this.nameResolver).resolve(passedIdentifier, EntityType.DOCUMENT,
                        new EntityReference("XWiki", EntityType.SPACE, new WikiReference("xwiki")));
                    will(returnValue(new DocumentReference("xwiki", targetSpace, "Admin")));
                    allowing(WikiUserManagerTest.this.nameResolver).resolve(passedIdentifier, EntityType.DOCUMENT,
                        new EntityReference("XWiki", EntityType.SPACE, new WikiReference("users")));
                    will(returnValue(new DocumentReference("users", targetSpace, "Admin")));
                    allowing(WikiUserManagerTest.this.nameResolver).resolve(passedIdentifier, EntityType.DOCUMENT,
                        new EntityReference("XWiki", EntityType.SPACE, new WikiReference("local")));
                    will(returnValue(new DocumentReference("local", targetSpace, "Admin")));
                }
            }
        });
    }
}
