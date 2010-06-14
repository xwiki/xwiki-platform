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
package org.xwiki.security.internal;

import org.xwiki.security.*;
import static org.xwiki.security.Right.*;
import static org.xwiki.security.RightState.*;

import static org.junit.Assert.*;
import org.junit.Test;
import org.jmock.Expectations;

import java.util.Collections;
import static java.util.Collections.EMPTY_LIST;
import static java.util.Arrays.asList;

import org.jmock.Mock;
import org.jmock.core.Invocation;
import org.jmock.core.stub.CustomStub;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;

import org.xwiki.observation.EventListener;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.user.api.XWikiGroupService;
import com.xpn.xwiki.user.api.XWikiRightNotFoundException;
import com.xpn.xwiki.user.api.XWikiRightService;

/**
 * Unit tests for {@link com.xpn.xwiki.user.impl.xwiki.XWikiRightServiceImpl}.
 * 
 * @version $Id: XWikiRightServiceImplTest.java 28526 2010-04-22 13:39:32Z tmortagne $
 */
public class XWikiRightServiceTest extends AbstractTestCase
{

    private XWikiContext getContext()
    {
        return xwikiContext;
    }

    @Test
    public void testHasAccessLevelWhithUserFromAnotherWiki() throws XWikiException
    {
        final MockDocument doc = new MockDocument("wiki2:Space.Page", "xwiki:XWiki.Admin");

        MockDocument preferences = new MockDocument("wiki2:XWiki.XWikiPreferences", "xwiki:XWiki.Admin");
        final XWikiRightService rightService = new XWikiCachingRightService();

        XWikiDocument user = new XWikiDocument(new DocumentReference("wiki", "XWiki", "user"));
        XWikiDocument group = new XWikiDocument(new DocumentReference("wiki", "XWiki", "group"));

        wiki.add(doc).add(preferences);

        mockery.checking(new Expectations() {{
            allowing(mockGroupService)
                .getAllGroupsNamesForMember("wiki:XWiki.user", Integer.MAX_VALUE, 0, xwikiContext);
            will(returnValue(asList(new String[]{"wiki:XWiki.group"})));
        }});

        getContext().setDatabase("wiki");

        assertFalse("User from another wiki has right on a local wiki", rightService.hasAccessLevel("view",
            user.getPrefixedFullName(), doc.getPrefixedFullName(), getContext()));

        // direct user rights

        preferences.allowGlobal(asList(new Right[]{VIEW }), asList(new String[]{user.getPrefixedFullName() }), EMPTY_LIST);

        ((EventListener) invalidator).onEvent(null, preferences, null);

        getContext().setDatabase(user.getWikiName());

        assertTrue("User from another wiki does not have right on a local wiki when tested from user wiki",
            rightService.hasAccessLevel("view", user.getPrefixedFullName(), doc.getPrefixedFullName(), 
                getContext()));
        assertTrue("User from another wiki does not have right on a local wiki when tested from user wiki",
            rightService.hasAccessLevel("view", user.getFullName(), doc.getPrefixedFullName(), 
                getContext()));

        getContext().setDatabase(doc.getWikiName());

        assertTrue("User from another wiki does not have right on a local wiki when tested from local wiki",
            rightService.hasAccessLevel("view", user.getPrefixedFullName(), doc.getPrefixedFullName(), 
                getContext()));
        assertTrue("User from another wiki does not have right on a local wiki when tested from local wiki",
            rightService.hasAccessLevel("view", user.getPrefixedFullName(), doc.getFullName(), 
                getContext()));

        // user group rights

        

        preferences = new MockDocument("wiki2:XWiki.XWikiPreferences", "xwiki:XWiki.Admin");
        preferences.allowGlobal(asList(new Right[]{VIEW}), EMPTY_LIST, asList(new String[]{group.getPrefixedFullName()}));
        wiki.add(preferences);
        
        ((EventListener) invalidator).onEvent(null, preferences, null);

        getContext().setDatabase(user.getWikiName());

        assertTrue("User group from another wiki does not have right on a local wiki when tested from user wiki",
            rightService.hasAccessLevel("view", user.getPrefixedFullName(), doc.getPrefixedFullName(), 
                getContext()));
        assertTrue("User group from another wiki does not have right on a local wiki when tested from user wiki",
            rightService.hasAccessLevel("view", user.getFullName(), doc.getPrefixedFullName(), 
                getContext()));

        getContext().setDatabase(doc.getWikiName());

        assertTrue("User group from another wiki does not have right on a local wiki when tested from local wiki",
            rightService.hasAccessLevel("view", user.getPrefixedFullName(), doc.getPrefixedFullName(), 
                getContext()));
        assertTrue("User group from another wiki does not have right on a local wiki when tested from local wiki",
            rightService.hasAccessLevel("view", user.getPrefixedFullName(), doc.getFullName(), 
                getContext()));

        // user is wiki owner

        preferences = new MockDocument("wiki2:XWiki.XWikiPreferences", "xwiki:XWiki.Admin");
        wiki.add(preferences);

        ((EventListener) invalidator).onEvent(null, preferences, null);

        wiki.setWikiOwner(doc.getWikiName(), user.getPrefixedFullName());

        getContext().setDatabase(user.getWikiName());

        assertTrue("Wiki owner from another wiki does not have right on a local wiki when tested from user wiki",
            rightService.hasAccessLevel("view", user.getPrefixedFullName(), doc.getPrefixedFullName(), 
                getContext()));
        assertTrue("Wiki owner group from another wiki does not have right on a local wiki when tested from user wiki",
            rightService.hasAccessLevel("view", user.getFullName(), doc.getPrefixedFullName(), 
                getContext()));

        getContext().setDatabase(doc.getWikiName());

        assertTrue(
            "Wiki owner group from another wiki does not have right on a local wiki when tested from local wiki",
            rightService.hasAccessLevel("view", user.getPrefixedFullName(), doc.getPrefixedFullName(), 
                getContext()));
        assertTrue(
            "Wiki owner group from another wiki does not have right on a local wiki when tested from local wiki",
            rightService.hasAccessLevel("view", user.getPrefixedFullName(), doc.getFullName(), 
                getContext()));
    }

    /**
     * Test that programming rights are checked on the context user when no context document is set.
     */
    @Test
    public void testProgrammingRightsWhenNoContextDocumentIsSet() throws XWikiException
    {
        final XWikiRightService rightService = new XWikiCachingRightService();
        // Setup an XWikiPreferences document granting programming rights to XWiki.Programmer
        MockDocument prefs = new MockDocument("XWiki.XWikiPreferences", "XWiki.Admin");
        prefs.allowGlobal(asList(new Right[]{PROGRAM, ADMIN}), asList(new String[]{"XWiki.Programmer" }), EMPTY_LIST);
        wiki.add(prefs);

        mockery.checking(new Expectations() {{
            allowing(mockGroupService)
                .getAllGroupsNamesForMember("xwiki:" + RightService.GUEST_USER_FULLNAME, Integer.MAX_VALUE, 0, xwikiContext);
            will(returnValue(EMPTY_LIST));
            allowing(mockGroupService)
                .getAllGroupsNamesForMember("xwiki:XWiki.Programmer", Integer.MAX_VALUE, 0, xwikiContext);
            will(returnValue(EMPTY_LIST));
            allowing(mockGroupService)
                .getAllGroupsNamesForMember("xwiki:XWiki.superadmin", Integer.MAX_VALUE, 0, xwikiContext);
            will(returnValue(EMPTY_LIST));
        }});

        // Setup the context (no context document)
        getContext().setMainXWiki("xwiki");
        
        getContext().remove("doc");
        getContext().remove("sdoc");
        getContext().setDatabase("xwiki");

        // XWiki.Programmer should have PR, as per the global rights.
        getContext().setUser("XWiki.Programmer");
        assertTrue(rightService.hasProgrammingRights(getContext()));

        // Guests should not have PR
        getContext().setUser(RightService.GUEST_USER_FULLNAME);
        assertFalse(rightService.hasProgrammingRights(getContext()));

        // superadmin should always have PR
        getContext().setUser(RightService.SUPERADMIN_USER_FULLNAME);
        assertTrue(rightService.hasProgrammingRights(getContext()));
    }
}
