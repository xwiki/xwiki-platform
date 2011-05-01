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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jmock.Expectations;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
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
import org.xwiki.users.UserManager;

/**
 * Tests the meta user manager.
 * 
 * @version $Id$
 * @since 3.1M2
 */
public class MetaUserManagerTest extends AbstractMockingComponentTestCase
{
    @MockingRequirement
    private MetaUserManager userManager;

    private EntityReferenceResolver<String> resolver;

    private EntityReferenceSerializer<String> serializer;

    private ConfigurationSource configuration;

    private ModelConfiguration modelConfiguration;

    @Before
    @Override
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception
    {
        super.setUp();
        this.resolver = getComponentManager().lookup(EntityReferenceResolver.class, "explicit");
        this.serializer = getComponentManager().lookup(EntityReferenceSerializer.class);
        this.configuration = getComponentManager().lookup(ConfigurationSource.class, "xwikiproperties");
        this.modelConfiguration = getComponentManager().lookup(ModelConfiguration.class);
    }

    @Test
    public void getUserFromUsernameWithNoManagers() throws Exception
    {
        final ComponentManager cm = getComponentManager().lookup(ComponentManager.class);
        final DocumentReference admin = new DocumentReference("xwiki", "XWiki", "Admin");
        getMockery().checking(new Expectations()
        {
            {
                allowing(cm).lookupMap(UserManager.class);
                will(returnValue(Collections.emptyMap()));

                allowing(MetaUserManagerTest.this.modelConfiguration).getDefaultReferenceValue(EntityType.WIKI);
                will(returnValue("xwiki"));

                allowing(MetaUserManagerTest.this.configuration).getProperty("users.defaultWiki", "xwiki");
                will(returnValue("xwiki"));

                exactly(1).of(MetaUserManagerTest.this.resolver).resolve("Admin", EntityType.DOCUMENT,
                    new EntityReference("XWiki", EntityType.SPACE, new WikiReference("xwiki")));
                will(returnValue(admin));

                exactly(1).of(MetaUserManagerTest.this.serializer).serialize(with(admin), with(new Object[0]));
                will(returnValue("xwiki:XWiki.Admin"));
            }
        });
        User u = this.userManager.getUser("Admin");
        Assert.assertEquals("xwiki:XWiki.Admin", u.getId());
        Assert.assertTrue(u instanceof InvalidUser);
    }

    @Test
    public void getUserFromFullReferenceWithNoManagers() throws Exception
    {
        final ComponentManager cm = getComponentManager().lookup(ComponentManager.class);
        final DocumentReference timmy = new DocumentReference("playground", "SouthPark", "Timmy");
        getMockery().checking(new Expectations()
        {
            {
                allowing(cm).lookupMap(UserManager.class);
                will(returnValue(Collections.emptyMap()));

                allowing(MetaUserManagerTest.this.modelConfiguration).getDefaultReferenceValue(EntityType.WIKI);
                will(returnValue("xwiki"));

                allowing(MetaUserManagerTest.this.configuration).getProperty("users.defaultWiki", "xwiki");
                will(returnValue("xwiki"));

                exactly(1).of(MetaUserManagerTest.this.resolver).resolve("playground:SouthPark.Timmy",
                    EntityType.DOCUMENT,
                    new EntityReference("XWiki", EntityType.SPACE, new WikiReference("xwiki")));
                will(returnValue(timmy));

                exactly(1).of(MetaUserManagerTest.this.serializer).serialize(with(timmy), with(new Object[0]));
                will(returnValue("playground:SouthPark.Timmy"));
            }
        });
        User u = this.userManager.getUser("playground:SouthPark.Timmy");
        Assert.assertEquals("playground:SouthPark.Timmy", u.getId());
        Assert.assertTrue(u instanceof InvalidUser);
    }

    @Test
    public void differentUsersWikiWithNoManagers() throws Exception
    {
        final ComponentManager cm = getComponentManager().lookup(ComponentManager.class);
        final DocumentReference admin = new DocumentReference("sandbox", "XWiki", "Admin");
        getMockery().checking(new Expectations()
        {
            {
                allowing(cm).lookupMap(UserManager.class);
                will(returnValue(Collections.emptyMap()));

                allowing(MetaUserManagerTest.this.modelConfiguration).getDefaultReferenceValue(EntityType.WIKI);
                will(returnValue("xwiki"));

                allowing(MetaUserManagerTest.this.configuration).getProperty("users.defaultWiki", "xwiki");
                will(returnValue("sandbox"));

                exactly(1).of(MetaUserManagerTest.this.resolver).resolve("Admin", EntityType.DOCUMENT,
                    new EntityReference("XWiki", EntityType.SPACE, new WikiReference("sandbox")));
                will(returnValue(admin));

                exactly(1).of(MetaUserManagerTest.this.serializer).serialize(with(admin), with(new Object[0]));
                will(returnValue("sandbox:XWiki.Admin"));
            }
        });
        User u = this.userManager.getUser("Admin");
        Assert.assertEquals("sandbox:XWiki.Admin", u.getId());
        Assert.assertTrue(u instanceof InvalidUser);
    }

    @Test
    public void differentGlobalWikiWithNoManagers() throws Exception
    {
        final ComponentManager cm = getComponentManager().lookup(ComponentManager.class);
        final DocumentReference admin = new DocumentReference("test", "XWiki", "Admin");
        getMockery().checking(new Expectations()
        {
            {
                allowing(cm).lookupMap(UserManager.class);
                will(returnValue(Collections.emptyMap()));

                allowing(MetaUserManagerTest.this.modelConfiguration).getDefaultReferenceValue(EntityType.WIKI);
                will(returnValue("test"));

                allowing(MetaUserManagerTest.this.configuration).getProperty("users.defaultWiki", "test");
                will(returnValue("test"));

                exactly(1).of(MetaUserManagerTest.this.resolver).resolve("Admin", EntityType.DOCUMENT,
                    new EntityReference("XWiki", EntityType.SPACE, new WikiReference("test")));
                will(returnValue(admin));

                exactly(1).of(MetaUserManagerTest.this.serializer).serialize(with(admin), with(new Object[0]));
                will(returnValue("test:XWiki.Admin"));
            }
        });
        User u = this.userManager.getUser("Admin");
        Assert.assertEquals("test:XWiki.Admin", u.getId());
        Assert.assertTrue(u instanceof InvalidUser);
    }

    @Test
    public void managerIteration() throws Exception
    {
        final ComponentManager cm = getComponentManager().lookup(ComponentManager.class);
        final User mockUser = getMockery().mock(User.class);
        final UserManager mockManager1 = getMockery().mock(UserManager.class, "um1");
        final UserManager mockManager2 = getMockery().mock(UserManager.class, "um2");
        final UserManager mockManager3 = getMockery().mock(UserManager.class, "um3");
        final Map<String, UserManager> managers = new LinkedHashMap<String, UserManager>();
        managers.put("1", mockManager1);
        managers.put("2", mockManager2);
        managers.put("3", mockManager3);
        getMockery().checking(new Expectations()
        {
            {
                allowing(cm).lookupMap(UserManager.class);
                will(returnValue(managers));

                exactly(1).of(mockManager1).getUser("Admin");
                will(returnValue(null));

                exactly(1).of(mockManager2).getUser("Admin");
                will(returnValue(mockUser));
            }
        });
        User u = this.userManager.getUser("Admin");
        Assert.assertSame(mockUser, u);
    }

    @Test
    public void managerIterationWithNoResults() throws Exception
    {
        final ComponentManager cm = getComponentManager().lookup(ComponentManager.class);
        final User mockUser = getMockery().mock(User.class);
        final UserManager mockManager1 = getMockery().mock(UserManager.class, "ldap");
        final UserManager mockManager2 = getMockery().mock(UserManager.class, "wiki");
        final Map<String, UserManager> managers = new LinkedHashMap<String, UserManager>();
        managers.put("ldap", mockManager1);
        managers.put("wiki", mockManager2);
        getMockery().checking(new Expectations()
        {
            {
                allowing(cm).lookupMap(UserManager.class);
                will(returnValue(managers));
                exactly(1).of(cm).lookup(UserManager.class, "wiki");
                will(returnValue(mockManager2));

                exactly(1).of(mockManager1).getUser("Admin");
                will(returnValue(null));

                exactly(1).of(mockManager2).getUser("Admin");
                will(returnValue(null));

                allowing(MetaUserManagerTest.this.configuration).getProperty("users.defaultUserManager", "wiki");
                will(returnValue("wiki"));

                exactly(1).of(mockManager2).getUser("Admin", true);
                will(returnValue(mockUser));
            }
        });
        User u = this.userManager.getUser("Admin", true);
        Assert.assertSame(mockUser, u);
    }

    @Test
    public void managerIterationWithNoResultsAndNoDefaultValue() throws Exception
    {
        final ComponentManager cm = getComponentManager().lookup(ComponentManager.class);
        final UserManager mockManager1 = getMockery().mock(UserManager.class, "1");
        final Map<String, UserManager> managers = new LinkedHashMap<String, UserManager>();
        final DocumentReference admin = new DocumentReference("test", "XWiki", "Admin");
        managers.put("1", mockManager1);
        getMockery().checking(new Expectations()
        {
            {
                allowing(cm).lookupMap(UserManager.class);
                will(returnValue(managers));
                exactly(1).of(cm).lookup(UserManager.class, "wiki");
                will(throwException(new ComponentLookupException("Missing Implementation")));

                exactly(1).of(mockManager1).getUser("Admin");
                will(returnValue(null));

                allowing(MetaUserManagerTest.this.configuration).getProperty("users.defaultUserManager", "wiki");
                will(returnValue("wiki"));
                allowing(MetaUserManagerTest.this.modelConfiguration).getDefaultReferenceValue(EntityType.WIKI);
                will(returnValue("test"));

                allowing(MetaUserManagerTest.this.configuration).getProperty("users.defaultWiki", "test");
                will(returnValue("test"));

                allowing(MetaUserManagerTest.this.configuration).getProperty("users.defaultUserManager", "wiki");
                will(returnValue("wiki"));

                exactly(1).of(MetaUserManagerTest.this.serializer).serialize(with(admin), with(new Object[0]));
                will(returnValue("test:XWiki.Admin"));

                exactly(1).of(MetaUserManagerTest.this.resolver).resolve("Admin", EntityType.DOCUMENT,
                    new EntityReference("XWiki", EntityType.SPACE, new WikiReference("test")));
                will(returnValue(admin));
            }
        });
        User u = this.userManager.getUser("Admin", true);
        Assert.assertEquals("test:XWiki.Admin", u.getId());
        Assert.assertTrue(u instanceof InvalidUser);
    }

    @Test
    public void lookupExceptions() throws Exception
    {
        final ComponentManager cm = getComponentManager().lookup(ComponentManager.class);
        final DocumentReference admin = new DocumentReference("test", "XWiki", "Admin");
        getMockery().checking(new Expectations()
        {
            {
                allowing(cm).lookupMap(UserManager.class);
                will(throwException(new ComponentLookupException("No Implementations")));
                allowing(MetaUserManagerTest.this.configuration).getProperty("users.defaultUserManager", "wiki");
                will(returnValue("wiki"));
                exactly(1).of(cm).lookup(UserManager.class, "wiki");
                will(throwException(new ComponentLookupException("Missing Implementation")));

                allowing(MetaUserManagerTest.this.modelConfiguration).getDefaultReferenceValue(EntityType.WIKI);
                will(returnValue("xwiki"));

                allowing(MetaUserManagerTest.this.configuration).getProperty("users.defaultWiki", "xwiki");
                will(returnValue("xwiki"));

                exactly(1).of(MetaUserManagerTest.this.resolver).resolve("Admin", EntityType.DOCUMENT,
                    new EntityReference("XWiki", EntityType.SPACE, new WikiReference("xwiki")));
                will(returnValue(admin));

                exactly(1).of(MetaUserManagerTest.this.serializer).serialize(with(admin), with(new Object[0]));
                will(returnValue("xwiki:XWiki.Admin"));
            }
        });
        User u = this.userManager.getUser("Admin", true);
        Assert.assertEquals("xwiki:XWiki.Admin", u.getId());
        Assert.assertTrue(u instanceof InvalidUser);
    }

    @Test
    public void differentDefaultUserManager() throws Exception
    {
        final ComponentManager cm = getComponentManager().lookup(ComponentManager.class);
        final DocumentReference admin = new DocumentReference("test", "XWiki", "Admin");
        getMockery().checking(new Expectations()
        {
            {
                allowing(cm).lookupMap(UserManager.class);
                will(returnValue(Collections.emptyMap()));
                allowing(MetaUserManagerTest.this.configuration).getProperty("users.defaultUserManager", "wiki");
                will(returnValue("ldap"));
                exactly(1).of(cm).lookup(UserManager.class, "ldap");
                will(throwException(new ComponentLookupException("Missing Implementation")));

                allowing(MetaUserManagerTest.this.modelConfiguration).getDefaultReferenceValue(EntityType.WIKI);
                will(returnValue("xwiki"));

                allowing(MetaUserManagerTest.this.configuration).getProperty("users.defaultWiki", "xwiki");
                will(returnValue("xwiki"));

                exactly(1).of(MetaUserManagerTest.this.resolver).resolve("Admin", EntityType.DOCUMENT,
                    new EntityReference("XWiki", EntityType.SPACE, new WikiReference("xwiki")));
                will(returnValue(admin));

                exactly(1).of(MetaUserManagerTest.this.serializer).serialize(with(admin), with(new Object[0]));
                will(returnValue("xwiki:XWiki.Admin"));
            }
        });
        User u = this.userManager.getUser("Admin", true);
        Assert.assertEquals("xwiki:XWiki.Admin", u.getId());
        Assert.assertTrue(u instanceof InvalidUser);
    }

    @Test
    public void nullIdentifierReturnsInvalidUser() throws Exception
    {
        User u = this.userManager.getUser(null);
        Assert.assertEquals("", u.getId());
        Assert.assertTrue(u instanceof InvalidUser);
    }

    @Test
    public void emptyIdentifierReturnsInvalidUser() throws Exception
    {
        User u = this.userManager.getUser("");
        Assert.assertEquals("", u.getId());
        Assert.assertTrue(u instanceof InvalidUser);
    }

    @Test
    public void blankIdentifierReturnsInvalidUser() throws Exception
    {
        User u = this.userManager.getUser("\n \t");
        Assert.assertEquals("", u.getId());
        Assert.assertTrue(u instanceof InvalidUser);
    }

    @Test
    public void blankIdentifierAndForceReturnsInvalidUser() throws Exception
    {
        User u = this.userManager.getUser("\n \t", true);
        Assert.assertEquals("", u.getId());
        Assert.assertTrue(u instanceof InvalidUser);
    }

    @Test
    public void forceWithMissingDefaultManager() throws Exception
    {
        final ComponentManager cm = getComponentManager().lookup(ComponentManager.class);
        final DocumentReference admin = new DocumentReference("test", "XWiki", "Admin");
        getMockery().checking(new Expectations()
        {
            {
                allowing(cm).lookupMap(UserManager.class);
                will(returnValue(Collections.emptyMap()));

                allowing(cm).lookup(UserManager.class, "wiki");
                will(throwException(new ComponentLookupException("Missing implementation")));

                allowing(MetaUserManagerTest.this.modelConfiguration).getDefaultReferenceValue(EntityType.WIKI);
                will(returnValue("test"));

                allowing(MetaUserManagerTest.this.configuration).getProperty("users.defaultWiki", "test");
                will(returnValue("test"));

                allowing(MetaUserManagerTest.this.configuration).getProperty("users.defaultUserManager", "wiki");
                will(returnValue("wiki"));

                exactly(1).of(MetaUserManagerTest.this.serializer).serialize(with(admin), with(new Object[0]));
                will(returnValue("test:XWiki.Admin"));

                exactly(1).of(MetaUserManagerTest.this.resolver).resolve("Admin", EntityType.DOCUMENT,
                    new EntityReference("XWiki", EntityType.SPACE, new WikiReference("test")));
                will(returnValue(admin));
            }
        });
        User u = this.userManager.getUser("Admin");
        Assert.assertEquals("test:XWiki.Admin", u.getId());
        Assert.assertTrue(u instanceof InvalidUser);
    }
}
