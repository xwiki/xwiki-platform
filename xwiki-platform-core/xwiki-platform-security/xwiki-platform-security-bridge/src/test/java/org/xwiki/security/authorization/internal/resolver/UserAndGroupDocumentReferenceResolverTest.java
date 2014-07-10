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
package org.xwiki.security.authorization.internal.resolver;

import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceValueProvider;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.annotation.AfterComponent;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

/**
 * Validate {@link UserAndGroupDocumentReferenceResolver}.
 * 
 * @version $Id$
 */
@AllComponents
public class UserAndGroupDocumentReferenceResolverTest
{
    @Rule
    public MockitoComponentMockingRule<DocumentReferenceResolver<String>> mocker =
        new MockitoComponentMockingRule<DocumentReferenceResolver<String>>(UserAndGroupDocumentReferenceResolver.class);

    @AfterComponent
    public void afterComponent() throws Exception
    {
        EntityReferenceValueProvider provider = this.mocker.registerMockComponent(EntityReferenceValueProvider.class);

        when(provider.getDefaultValue(EntityType.WIKI)).thenReturn("defaultwiki");
    }

    @Test
    public void testResolver() throws ComponentLookupException
    {
        Assert.assertTrue(this.mocker.getComponentUnderTest().resolve("Bosse")
            .equals(new DocumentReference("defaultwiki", "XWiki", "Bosse")));
        Assert.assertTrue(this.mocker.getComponentUnderTest().resolve("bossesSpace.Bosse")
            .equals(new DocumentReference("defaultwiki", "bossesSpace", "Bosse")));
        Assert.assertTrue(this.mocker.getComponentUnderTest().resolve("Bosse", new WikiReference("bossesWiki"))
            .equals(new DocumentReference("bossesWiki", "XWiki", "Bosse")));
        Assert.assertTrue(this.mocker.getComponentUnderTest()
            .resolve("bossesSpace.Bosse", new WikiReference("bossesWiki"))
            .equals(new DocumentReference("bossesWiki", "bossesSpace", "Bosse")));
        Assert.assertTrue(this.mocker.getComponentUnderTest().resolve("bossesWiki:bossesSpace.Bosse")
            .equals(new DocumentReference("bossesWiki", "bossesSpace", "Bosse")));
    }
}
