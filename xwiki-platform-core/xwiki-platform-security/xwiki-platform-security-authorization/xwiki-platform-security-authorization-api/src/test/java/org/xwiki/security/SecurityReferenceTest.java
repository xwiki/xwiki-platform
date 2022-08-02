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

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.model.EntityType;
import org.xwiki.model.internal.reference.EntityReferenceFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.internal.XWikiBridge;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Security Reference Unit Tests.
 *
 * @version $Id$
 * @since 4.0M2
 */
@ComponentTest
@ComponentList({EntityReferenceFactory.class})
public class SecurityReferenceTest
{
    private EntityReference xwiki = new EntityReference("xwiki", EntityType.WIKI);

    private EntityReference wiki = new EntityReference("wiki", EntityType.WIKI);

    private EntityReference xspace =
        new EntityReference("space", EntityType.SPACE, new EntityReference("xwiki", EntityType.WIKI));

    private EntityReference space =
        new EntityReference("space", EntityType.SPACE, new EntityReference("wiki", EntityType.WIKI));

    private EntityReference subEntity = new EntityReference("page", EntityType.DOCUMENT, space);

    private EntityReference mainEntity = new EntityReference("page", EntityType.DOCUMENT, xspace);

    private SpaceReference xwikispace = new SpaceReference("XWiki", xwiki);

    private SpaceReference wikispace = new SpaceReference("XWiki", wiki);

    private DocumentReference userRef = new DocumentReference("user1", xwikispace);

    private DocumentReference anotherWikiUserRef = new DocumentReference("user2", wikispace);

    private DocumentReference groupRef = new DocumentReference("group1", xwikispace);

    private DocumentReference anotherWikiGroupRef = new DocumentReference("group2", wikispace);

    @MockComponent
    private XWikiBridge xwikiBridge;

    @InjectMockComponents
    private DefaultSecurityReferenceFactory factory;

    @BeforeEach
    public void beforeEach() throws Exception
    {
        when(xwikiBridge.getMainWikiReference()).thenReturn(new WikiReference("xwiki"));
        when(xwikiBridge.toCompatibleEntityReference(any(EntityReference.class)))
            .thenAnswer(new Answer<EntityReference>()
            {
                @Override
                public EntityReference answer(InvocationOnMock invocation) throws Throwable
                {
                    return invocation.getArgument(0);
                }
            });
    }

    @Test
    public void testEquality() throws Exception
    {
        assertThat(factory.newEntityReference(mainEntity), equalTo(factory.newEntityReference(mainEntity)));
        assertThat(factory.newEntityReference(subEntity), equalTo(factory.newEntityReference(subEntity)));
        assertThat(factory.newEntityReference(mainEntity), not(equalTo(factory.newEntityReference(subEntity))));
        assertThat(factory.newEntityReference(subEntity), not(equalTo(factory.newEntityReference(mainEntity))));
    }

    @Test
    public void testGetReversedSecurityReferenceChain() throws Exception
    {
        List<SecurityReference> subList =
            (List<SecurityReference>) factory.newEntityReference(subEntity).getReversedSecurityReferenceChain();
        assertThat(subList.get(0), equalTo(xwiki));
        assertThat(subList.get(0).getOriginalReference(), equalTo(xwiki));
        assertThat(subList.get(1), equalTo(wiki));
        assertThat(subList.get(1).getOriginalReference(), equalTo(wiki));
        assertThat(subList.get(2), equalTo(space));
        assertThat(subList.get(2).getOriginalReference(), equalTo(space));
        assertThat(subList.get(3), equalTo(subEntity));
        assertThat(subList.get(3).getOriginalReference(), equalTo(subEntity));

        List<SecurityReference> mainList =
            (List<SecurityReference>) factory.newEntityReference(mainEntity).getReversedSecurityReferenceChain();
        assertThat(mainList.get(0), equalTo(xwiki));
        assertThat(mainList.get(0).getOriginalReference(), equalTo(xwiki));
        assertThat(mainList.get(1), equalTo(xspace));
        assertThat(mainList.get(1).getOriginalReference(), equalTo(xspace));
        assertThat(mainList.get(2), equalTo(mainEntity));
        assertThat(mainList.get(2).getOriginalReference(), equalTo(mainEntity));
    }

    @Test
    public void testSecurityReferenceForNullReference() throws Exception
    {
        assertThat(factory.newEntityReference(null), equalTo(factory.newEntityReference(xwiki)));
        assertThat(factory.newUserReference(null), equalTo(factory.newEntityReference(xwiki)));
        assertThat(factory.newEntityReference(null).getOriginalReference(), equalTo(xwiki));
        assertThat(factory.newUserReference(null).getOriginalDocumentReference(), nullValue());

        assertThrows(IllegalArgumentException.class, () -> {
            factory.newGroupReference(null);
            // never reached !!
        });
    }

    @Test
    public void testGetSecurityType() throws Exception
    {
        assertThat(factory.newEntityReference(null).getSecurityType(), equalTo(SecurityReference.FARM));
        assertThat(factory.newEntityReference(xwiki).getSecurityType(), equalTo(SecurityReference.FARM));
        assertThat(factory.newEntityReference(wiki).getSecurityType(), equalTo(EntityType.WIKI));
        assertThat(factory.newEntityReference(xspace).getSecurityType(), equalTo(EntityType.SPACE));
        assertThat(factory.newEntityReference(space).getSecurityType(), equalTo(EntityType.SPACE));
        assertThat(factory.newEntityReference(mainEntity).getSecurityType(), equalTo(EntityType.DOCUMENT));
        assertThat(factory.newEntityReference(subEntity).getSecurityType(), equalTo(EntityType.DOCUMENT));
    }

    @Test
    public void testGetOriginalWikiReference() throws Exception
    {
        assertThat(factory.newEntityReference(null).getOriginalWikiReference(), equalTo(xwiki));
        assertThat(factory.newEntityReference(xwiki).getOriginalWikiReference(), equalTo(xwiki));
        assertThat(factory.newEntityReference(wiki).getOriginalWikiReference(), equalTo(wiki));
        assertThat(factory.newEntityReference(xspace).getOriginalWikiReference(), nullValue());
        assertThat(factory.newEntityReference(space).getOriginalWikiReference(), nullValue());
        assertThat(factory.newEntityReference(mainEntity).getOriginalWikiReference(), nullValue());
        assertThat(factory.newEntityReference(subEntity).getOriginalWikiReference(), nullValue());
    }

    @Test
    public void testGetOriginalSpaceReference() throws Exception
    {
        assertThat(factory.newEntityReference(null).getOriginalSpaceReference(), nullValue());
        assertThat(factory.newEntityReference(xwiki).getOriginalSpaceReference(), nullValue());
        assertThat(factory.newEntityReference(wiki).getOriginalSpaceReference(), nullValue());
        assertThat(factory.newEntityReference(xspace).getOriginalSpaceReference(), equalTo(xspace));
        assertThat(factory.newEntityReference(space).getOriginalSpaceReference(), equalTo(space));
        assertThat(factory.newEntityReference(mainEntity).getOriginalSpaceReference(), nullValue());
        assertThat(factory.newEntityReference(subEntity).getOriginalSpaceReference(), nullValue());
    }

    @Test
    public void testGetOriginalDocumentReference() throws Exception
    {
        assertThat(factory.newEntityReference(null).getOriginalDocumentReference(), nullValue());
        assertThat(factory.newEntityReference(xwiki).getOriginalDocumentReference(), nullValue());
        assertThat(factory.newEntityReference(wiki).getOriginalDocumentReference(), nullValue());
        assertThat(factory.newEntityReference(xspace).getOriginalDocumentReference(), nullValue());
        assertThat(factory.newEntityReference(space).getOriginalDocumentReference(), nullValue());
        assertThat(factory.newEntityReference(mainEntity).getOriginalDocumentReference(), equalTo(mainEntity));
        assertThat(factory.newEntityReference(subEntity).getOriginalDocumentReference(), equalTo(subEntity));
    }

    @Test
    public void testIsGlobal() throws Exception
    {
        assertThat(factory.newUserReference(userRef).isGlobal(), is(true));
        assertThat(factory.newUserReference(anotherWikiUserRef).isGlobal(), is(false));
        assertThat(factory.newGroupReference(groupRef).isGlobal(), is(true));
        assertThat(factory.newGroupReference(anotherWikiGroupRef).isGlobal(), is(false));
    }

    @Test
    public void testGetWikiReference() throws Exception
    {
        assertThat(factory.newUserReference(userRef).getWikiReference(), equalTo(xwiki));
        assertThat(factory.newUserReference(anotherWikiUserRef).getWikiReference(), equalTo(wiki));
        assertThat(factory.newEntityReference(mainEntity).getWikiReference(), equalTo(xwiki));
        assertThat(factory.newEntityReference(subEntity).getWikiReference(), equalTo(wiki));
        assertThat(factory.newEntityReference(xwiki).getWikiReference(), equalTo(xwiki));
        assertThat(factory.newEntityReference(wiki).getWikiReference(), equalTo(wiki));
    }
}
