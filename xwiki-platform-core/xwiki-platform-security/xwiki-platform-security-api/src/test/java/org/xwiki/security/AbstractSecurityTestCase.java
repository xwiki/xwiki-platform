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
package org.xwiki.security;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jmock.Expectations;
import org.junit.Before;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.internal.XWikiBridge;
import org.xwiki.test.AbstractMockingComponentTestCase;

/**
 * A base class for security tests, defining some useful SecurityReference.
 *
 * @version $Id$
 * @since 4.0M2
 */
public class AbstractSecurityTestCase extends AbstractMockingComponentTestCase
{
    protected SecurityReference xwikiRef;
    protected SecurityReference wikiRef;
    protected SecurityReference anotherWikiRef;
    protected SecurityReference xspaceRef;
    protected SecurityReference anotherXspaceRef;
    protected SecurityReference spaceRef;
    protected SecurityReference anotherSpaceRef;
    protected SecurityReference anotherWikiSpaceRef;
    protected SecurityReference xdocRef;
    protected SecurityReference anotherXdocRef;
    protected SecurityReference docRef;
    protected SecurityReference anotherDocRef;
    protected SecurityReference anotherSpaceDocRef;
    protected SecurityReference anotherWikiDocRef;

    protected List<SecurityReference> entityRefs;

    protected SecurityReference xXWikiSpace;
    protected SecurityReference xwikiSpace;
    protected SecurityReference anotherWikiXWikiSpace;

    protected List<SecurityReference> xwikiSpaceRefs;

    protected UserSecurityReference xuserRef;
    protected UserSecurityReference userRef;
    protected UserSecurityReference creatorRef;
    protected UserSecurityReference ownerRef;
    protected UserSecurityReference anotherXuserRef;
    protected UserSecurityReference anotherUserRef;
    protected UserSecurityReference anotherWikiUserRef;
    protected UserSecurityReference defaultUserRef;

    protected GroupSecurityReference groupRef;
    protected GroupSecurityReference anotherGroupRef;

    protected UserSecurityReference groupUserRef;
    protected UserSecurityReference anotherGroupUserRef;
    protected UserSecurityReference bothGroupUserRef;

    protected List<UserSecurityReference> userRefs;
    protected List<UserSecurityReference> groupUserRefs;

    protected Map<GroupSecurityReference, List<UserSecurityReference>> groupRefs
        = new HashMap<GroupSecurityReference, List<UserSecurityReference>>();

    /** Cache the main wiki reference. */
    private SecurityReference mainWikiReference;

    /** @return the main wiki reference. */
    private SecurityReference getMainWikiReference()
    {
        if (mainWikiReference == null) {
            mainWikiReference = new SecurityReference(new WikiReference("xwiki"));
        }
        return mainWikiReference;
    }

    private SecurityReference newEntityReference(EntityReference reference)
    {
        return new SecurityReference(reference, getMainWikiReference());
    }

    private UserSecurityReference newUserReference(DocumentReference reference)
    {
        return new UserSecurityReference(reference, getMainWikiReference());
    }

    private GroupSecurityReference newGroupReference(DocumentReference reference)
    {
        return new GroupSecurityReference(reference, getMainWikiReference());
    }

    @Before
    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        final XWikiBridge xwikiBridge = getComponentManager().getInstance(XWikiBridge.class);

        getMockery().checking(new Expectations() {{
            allowing (xwikiBridge).getMainWikiReference(); will(returnValue(new WikiReference("xwiki")));
        }});

        xwikiRef = newEntityReference(new WikiReference("xwiki"));
        wikiRef = newEntityReference(new WikiReference("wiki"));
        anotherWikiRef = newEntityReference(new WikiReference("anotherWiki"));

        xspaceRef = newEntityReference(new SpaceReference("space", xwikiRef.getOriginalWikiReference()));
        anotherXspaceRef = newEntityReference(new SpaceReference("anotherSpace",
            xwikiRef.getOriginalWikiReference()));
        spaceRef = newEntityReference(new SpaceReference("space", wikiRef.getOriginalWikiReference()));
        anotherSpaceRef = newEntityReference(new SpaceReference("anotherSpace",
            wikiRef.getOriginalWikiReference()));
        anotherWikiSpaceRef = newEntityReference(new SpaceReference("space",
            anotherWikiRef.getOriginalWikiReference()));

        xdocRef = newEntityReference(new DocumentReference("page", xspaceRef.getOriginalSpaceReference()));
        anotherXdocRef = newEntityReference(new DocumentReference("anotherPage",
            xspaceRef.getOriginalSpaceReference()));
        docRef = newEntityReference(new DocumentReference("page", spaceRef.getOriginalSpaceReference()));
        anotherDocRef = newEntityReference(new DocumentReference("anotherPage",
            spaceRef.getOriginalSpaceReference()));
        anotherSpaceDocRef = newEntityReference(new DocumentReference("page",
            anotherSpaceRef.getOriginalSpaceReference()));
        anotherWikiDocRef = newEntityReference(new DocumentReference("page",
            anotherWikiSpaceRef.getOriginalSpaceReference()));

        xXWikiSpace = newEntityReference(new SpaceReference("XWiki", xwikiRef.getOriginalWikiReference()));
        xwikiSpace = newEntityReference(new SpaceReference("XWiki", wikiRef.getOriginalWikiReference()));
        anotherWikiXWikiSpace = newEntityReference(new SpaceReference("XWiki",
            anotherWikiRef.getOriginalWikiReference()));

        xuserRef = newUserReference(new DocumentReference("user", xXWikiSpace.getOriginalSpaceReference()));
        ownerRef = newUserReference(new DocumentReference("owner", xXWikiSpace.getOriginalSpaceReference()));
        userRef = newUserReference(new DocumentReference("user", xwikiSpace.getOriginalSpaceReference()));
        creatorRef = newUserReference(new DocumentReference("creator", xwikiSpace.getOriginalSpaceReference()));
        anotherXuserRef = newUserReference(new DocumentReference("anotherUser",
            xXWikiSpace.getOriginalSpaceReference()));
        anotherUserRef = newUserReference(new DocumentReference("anotherUser",
            xwikiSpace.getOriginalSpaceReference()));
        anotherWikiUserRef = newUserReference(new DocumentReference("user",
            anotherWikiXWikiSpace.getOriginalSpaceReference()));
        defaultUserRef = newUserReference(new DocumentReference("default", xwikiSpace.getOriginalSpaceReference()));

        groupRef = newGroupReference(new DocumentReference("group", xwikiSpace.getOriginalSpaceReference()));
        anotherGroupRef = newGroupReference(
            new DocumentReference("anotherGroup", xwikiSpace.getOriginalSpaceReference()));

        groupUserRef = newUserReference(new DocumentReference("groupUser", xwikiSpace.getOriginalSpaceReference()));
        anotherGroupUserRef = newUserReference(new DocumentReference("anotherGroupUser", xwikiSpace.getOriginalSpaceReference()));
        bothGroupUserRef = newUserReference(new DocumentReference("bothGroupUserRef", xwikiSpace.getOriginalSpaceReference()));

        entityRefs = Arrays.asList(xwikiRef, wikiRef, xspaceRef, spaceRef, xdocRef, docRef, anotherXspaceRef,
            anotherXdocRef, anotherSpaceRef, anotherSpaceDocRef, anotherDocRef, anotherWikiRef, anotherWikiSpaceRef,
            anotherWikiDocRef);

        xwikiSpaceRefs = Arrays.asList(xXWikiSpace, xwikiSpace, anotherWikiXWikiSpace);

        userRefs = Arrays.asList(xuserRef, userRef, anotherXuserRef, anotherUserRef, anotherWikiUserRef);

        groupUserRefs = Arrays.asList(groupUserRef, anotherGroupUserRef, bothGroupUserRef);
        groupRefs.put(groupRef, Arrays.asList(groupUserRef, bothGroupUserRef));
        groupRefs.put(anotherGroupRef, Arrays.asList(anotherGroupUserRef, bothGroupUserRef));
    }
}
