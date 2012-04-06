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
package org.xwiki.security.authorization.internal;

import java.util.Collections;

import org.jmock.Expectations;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.observation.EventListener;
import org.xwiki.security.authorization.AuthorizationManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.user.api.XWikiRightService;

import junit.framework.Assert;

import static java.util.Arrays.asList;
import static org.xwiki.security.authorization.Right.ADMIN;
import static org.xwiki.security.authorization.Right.PROGRAM;
import static org.xwiki.security.authorization.Right.VIEW;

import org.xwiki.environment.Environment;

/**
 * Unit tests for {@link com.xpn.xwiki.user.impl.xwiki.XWikiRightServiceImpl}.
 * 
 * @version $Id$
 */
public class XWikiRightServiceTest extends AbstractTestCase
{
    /**
     * Prefix for generating full user names.
     */
    private static final String XWIKI_SPACE_PREFIX = "XWiki.";

    /**
     * The Superadmin full name.
     */
    private static final String SUPERADMIN_USER_FULLNAME = XWIKI_SPACE_PREFIX + AuthorizationManager.SUPERADMIN_USER;

    /**
     * The Guest username.
     */
    private static final String GUEST_USER = "XWikiGuest";

    /**
     * The Guest full name.
     */
    private static final String GUEST_USER_FULLNAME = XWIKI_SPACE_PREFIX + GUEST_USER;


    /**
     * String document reference resolver.
     */
    private DocumentReferenceResolver<String> documentReferenceResolver;

    private XWikiContext getContext()
    {
        return xwikiContext;
    }

    @Override
    protected void registerComponents() throws Exception
    {
        getComponentManager().unregisterComponent(Environment.class, "default");
        documentReferenceResolver = getComponentManager().getInstance(DocumentReferenceResolver.TYPE_STRING);
    }

    @Test
    public void testHasAccessLevelWhithUserFromAnotherWiki() throws XWikiException
    {
        final MockDocument doc = new MockDocument(documentReferenceResolver.resolve("wiki2:Space.Page"), "xwiki:XWiki.Admin");

        MockDocument preferences = new MockDocument(documentReferenceResolver.resolve("wiki2:XWiki.XWikiPreferences"), "xwiki:XWiki.Admin");
        final XWikiRightService rightService = new XWikiCachingRightService();

        final XWikiDocument user = new XWikiDocument(new DocumentReference("wiki", "XWiki", "user"));
        final XWikiDocument group = new XWikiDocument(new DocumentReference("wiki", "XWiki", "group"));

        wiki.add(doc).add(preferences);

        getMockery().checking(new Expectations() {{
            allowing(mockGroupService)
                .getAllGroupsReferencesForMember(user.getDocumentReference(), 0, 0, xwikiContext);
            will(Expectations.returnValue(asList(group.getDocumentReference())));
        }});

        getContext().setDatabase("wiki");

        Assert.assertFalse("User from another wiki has right on a local wiki", rightService.hasAccessLevel("view",
            user.getPrefixedFullName(), doc.getPrefixedFullName(), getContext()));

        // direct user rights

        preferences.allowGlobal(asList(VIEW), asList(user.getPrefixedFullName()), Collections.<String>emptyList());

        ((EventListener) rulesInvalidator).onEvent(null, preferences, null);

        getContext().setDatabase(user.getWikiName());

        Assert.assertTrue("User from another wiki does not have right on a local wiki when tested from user wiki",
            rightService.hasAccessLevel("view", user.getPrefixedFullName(), doc.getPrefixedFullName(),
                getContext()));
        Assert.assertTrue("User from another wiki does not have right on a local wiki when tested from user wiki",
            rightService.hasAccessLevel("view", user.getFullName(), doc.getPrefixedFullName(),
                getContext()));

        getContext().setDatabase(doc.getWikiName());

        Assert.assertTrue("User from another wiki does not have right on a local wiki when tested from local wiki",
            rightService.hasAccessLevel("view", user.getPrefixedFullName(), doc.getPrefixedFullName(),
                getContext()));
        Assert.assertTrue("User from another wiki does not have right on a local wiki when tested from local wiki",
            rightService.hasAccessLevel("view", user.getPrefixedFullName(), doc.getFullName(),
                getContext()));

        // user group rights

        

        preferences = new MockDocument(documentReferenceResolver.resolve("wiki2:XWiki.XWikiPreferences"), "xwiki:XWiki.Admin");
        preferences.allowGlobal(asList(VIEW), Collections.<String>emptyList(), asList(group.getPrefixedFullName()));
        wiki.add(preferences);
        
        ((EventListener) rulesInvalidator).onEvent(null, preferences, null);

        getContext().setDatabase(user.getWikiName());

        Assert.assertTrue("User group from another wiki does not have right on a local wiki when tested from user wiki",
            rightService.hasAccessLevel("view", user.getPrefixedFullName(), doc.getPrefixedFullName(),
                getContext()));
        Assert.assertTrue("User group from another wiki does not have right on a local wiki when tested from user wiki",
            rightService.hasAccessLevel("view", user.getFullName(), doc.getPrefixedFullName(),
                getContext()));

        getContext().setDatabase(doc.getWikiName());

        Assert
            .assertTrue("User group from another wiki does not have right on a local wiki when tested from local wiki",
                rightService.hasAccessLevel("view", user.getPrefixedFullName(), doc.getPrefixedFullName(),
                    getContext()));
        Assert
            .assertTrue("User group from another wiki does not have right on a local wiki when tested from local wiki",
                rightService.hasAccessLevel("view", user.getPrefixedFullName(), doc.getFullName(),
                    getContext()));

        // user is wiki owner

        preferences = new MockDocument(documentReferenceResolver.resolve("wiki2:XWiki.XWikiPreferences"), "xwiki:XWiki.Admin");
        wiki.add(preferences);

        ((EventListener) rulesInvalidator).onEvent(null, preferences, null);

        wiki.setWikiOwner(doc.getWikiName(), user.getPrefixedFullName());

        getContext().setDatabase(user.getWikiName());

        Assert.assertTrue("Wiki owner from another wiki does not have right on a local wiki when tested from user wiki",
            rightService.hasAccessLevel("view", user.getPrefixedFullName(), doc.getPrefixedFullName(),
                getContext()));
        Assert.assertTrue(
            "Wiki owner group from another wiki does not have right on a local wiki when tested from user wiki",
            rightService.hasAccessLevel("view", user.getFullName(), doc.getPrefixedFullName(),
                getContext()));

        getContext().setDatabase(doc.getWikiName());

        Assert.assertTrue(
            "Wiki owner group from another wiki does not have right on a local wiki when tested from local wiki",
            rightService.hasAccessLevel("view", user.getPrefixedFullName(), doc.getPrefixedFullName(),
                getContext()));
        Assert.assertTrue(
            "Wiki owner group from another wiki does not have right on a local wiki when tested from local wiki",
            rightService.hasAccessLevel("view", user.getPrefixedFullName(), doc.getFullName(),
                getContext()));
    }

    /**
     * Test that programming rights are checked on the context user when no context document is set.
     * @throws com.xpn.xwiki.XWikiException on error
     */
    @Test
    public void testProgrammingRightsWhenNoContextDocumentIsSet() throws XWikiException
    {
        final XWikiRightService rightService = new XWikiCachingRightService();
        // Setup an XWikiPreferences document granting programming rights to XWiki.Programmer
        MockDocument prefs = new MockDocument(documentReferenceResolver.resolve("XWiki.XWikiPreferences"), "XWiki.Admin");
        prefs.allowGlobal(asList(PROGRAM, ADMIN), asList("XWiki.Programmer"), Collections.<String>emptyList());
        wiki.add(prefs);

        getMockery().checking(new Expectations() {{
            allowing(mockGroupService)
                .getAllGroupsReferencesForMember(
                    new DocumentReference("xwiki", XWikiConstants.WIKI_SPACE, XWikiConstants.GUEST_USER),
                    0, 0, xwikiContext);
            will(Expectations.returnValue(Collections.emptyList()));
            allowing(mockGroupService)
                .getAllGroupsReferencesForMember(
                    new DocumentReference("xwiki", XWikiConstants.WIKI_SPACE, "Programmer"),
                    0, 0, xwikiContext);
            will(Expectations.returnValue(Collections.emptyList()));
            allowing(mockGroupService)
                .getAllGroupsReferencesForMember(
                    new DocumentReference("xwiki", XWikiConstants.WIKI_SPACE, "superadmin"),
                    0, 0, xwikiContext);
            will(Expectations.returnValue(Collections.emptyList()));
        }});

        // Setup the context (no context document)
        getContext().setMainXWiki("xwiki");
        
        getContext().remove("doc");
        getContext().remove("sdoc");
        getContext().setDatabase("xwiki");

        // XWiki.Programmer should have PR, as per the global rights.
        getContext().setUser("XWiki.Programmer");
        Assert.assertTrue(rightService.hasProgrammingRights(getContext()));

        // Guests should not have PR
        getContext().setUser(XWikiConstants.GUEST_USER_FULLNAME);
        Assert.assertFalse(rightService.hasProgrammingRights(getContext()));

        // superadmin should always have PR
        getContext().setUser(XWikiConstants.WIKI_SPACE + '.' + AuthorizationManager.SUPERADMIN_USER);
        Assert.assertTrue(rightService.hasProgrammingRights(getContext()));
    }
}
