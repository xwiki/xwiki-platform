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

package org.xwiki.security.authorization;

import org.jmock.Expectations;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.DefaultSecurityReferenceFactory;
import org.xwiki.security.SecurityReferenceFactory;
import org.xwiki.security.internal.XWikiBridge;
import org.xwiki.test.jmock.AbstractMockingComponentTestCase;
import org.xwiki.test.jmock.annotation.MockingRequirement;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Security Reference Unit Tests
 *
 * @version $Id$
 * @since 5.0M2
 */
@MockingRequirement(DefaultSecurityReferenceFactory.class)
public class UserSecurityReferenceTest extends AbstractMockingComponentTestCase<SecurityReferenceFactory>
{
    private WikiReference xwiki = new WikiReference("xwiki");
    private WikiReference wiki = new WikiReference("wiki");
    private SpaceReference xspace = new SpaceReference("XWiki", xwiki);
    private SpaceReference space = new SpaceReference("XWiki", wiki);
    private DocumentReference userRef = new DocumentReference("user1", xspace);
    private DocumentReference anotherWikiUserRef = new DocumentReference("user2", space);
    private DocumentReference groupRef = new DocumentReference("group1", xspace);
    private DocumentReference anotherWikiGroupRef = new DocumentReference("group2", space);

    @Before
    public void configure() throws Exception
    {
        final XWikiBridge wikiBridge = getComponentManager().getInstance(XWikiBridge.class);

        getMockery().checking(new Expectations()
        {{
                allowing(wikiBridge).getMainWikiReference();
                will(returnValue(new WikiReference("xwiki")));
        }});
    }

    @Test
    public void testIsGlobal() throws Exception
    {
        assertThat(getMockedComponent().newUserReference(userRef).isGlobal(), is(true));
        assertThat(getMockedComponent().newUserReference(anotherWikiUserRef).isGlobal(), is(false));
        assertThat(getMockedComponent().newGroupReference(groupRef).isGlobal(), is(true));
        assertThat(getMockedComponent().newGroupReference(anotherWikiGroupRef).isGlobal(), is(false));
    }
}
