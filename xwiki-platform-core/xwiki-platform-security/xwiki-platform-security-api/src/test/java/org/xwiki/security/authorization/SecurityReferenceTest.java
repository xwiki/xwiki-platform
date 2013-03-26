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

import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.DefaultSecurityReferenceFactory;
import org.xwiki.security.SecurityReference;
import org.xwiki.security.SecurityReferenceFactory;
import org.xwiki.security.internal.XWikiBridge;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

/**
 * Security Reference Unit Tests.
 *
 * @version $Id$
 * @since 4.0M2
 */
public class SecurityReferenceTest
{
    @Rule
    public final MockitoComponentMockingRule<SecurityReferenceFactory> securityReferenceFactoryMocker =
        new MockitoComponentMockingRule<SecurityReferenceFactory>(DefaultSecurityReferenceFactory.class);

    private EntityReference xwiki = new EntityReference("xwiki", EntityType.WIKI);
    private EntityReference wiki = new EntityReference("wiki", EntityType.WIKI);
    private EntityReference xspace = new EntityReference("space", EntityType.SPACE,
        new EntityReference("xwiki", EntityType.WIKI));
    private EntityReference space = new EntityReference("space", EntityType.SPACE,
        new EntityReference("wiki", EntityType.WIKI));
    private EntityReference subEntity = new EntityReference("page", EntityType.DOCUMENT, space);
    private EntityReference mainEntity = new EntityReference("page", EntityType.DOCUMENT, xspace);
    
    private SecurityReferenceFactory factory;
    
    @Before
    public void configure() throws Exception
    {
        XWikiBridge xwikiBridge = securityReferenceFactoryMocker.getInstance(XWikiBridge.class);
        when(xwikiBridge.getMainWikiReference()).thenReturn(new WikiReference("xwiki"));
        
        factory = securityReferenceFactoryMocker.getComponentUnderTest();
    }

    @Test
    public void testEquality() throws Exception
    {
        assertThat(factory.newEntityReference(mainEntity),
            equalTo(factory.newEntityReference(mainEntity)));
        assertThat(factory.newEntityReference(subEntity),
            equalTo(factory.newEntityReference(subEntity)));
        assertThat(factory.newEntityReference(mainEntity),
            not(equalTo(factory.newEntityReference(subEntity))));
        assertThat(factory.newEntityReference(subEntity),
            not(equalTo(factory.newEntityReference(mainEntity))));
    }

    @Test
    public void testGetReversedSecurityReferenceChain() throws Exception
    {
        List<SecurityReference> subList = (List<SecurityReference>) factory.newEntityReference(subEntity)
            .getReversedSecurityReferenceChain();
        assertThat(subList.get(0), equalTo(xwiki));
        assertThat(subList.get(0).getOriginalReference(), equalTo(xwiki));
        assertThat(subList.get(1), equalTo(wiki));
        assertThat(subList.get(1).getOriginalReference(), equalTo(wiki));
        assertThat(subList.get(2), equalTo(space));
        assertThat(subList.get(2).getOriginalReference(), equalTo(space));
        assertThat(subList.get(3), equalTo(subEntity));
        assertThat(subList.get(3).getOriginalReference(), equalTo(subEntity));

        List<SecurityReference> mainList = (List<SecurityReference>) factory.newEntityReference(mainEntity)
            .getReversedSecurityReferenceChain();
        assertThat(mainList.get(0), equalTo(xwiki));
        assertThat(mainList.get(0).getOriginalReference(), equalTo(xwiki));
        assertThat(mainList.get(1), equalTo(xspace));
        assertThat(mainList.get(1).getOriginalReference(), equalTo(xspace));
        assertThat(mainList.get(2), equalTo(mainEntity));
        assertThat(mainList.get(2).getOriginalReference(), equalTo(mainEntity));
    }
}
