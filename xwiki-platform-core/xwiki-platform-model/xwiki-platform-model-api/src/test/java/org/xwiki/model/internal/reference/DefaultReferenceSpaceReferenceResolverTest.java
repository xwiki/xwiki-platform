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
package org.xwiki.model.internal.reference;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.SpaceReferenceResolver;
import org.xwiki.model.reference.WikiReference;

/**
 * Unit tests for {@link org.xwiki.model.internal.reference.DefaultReferenceSpaceReferenceResolver}.
 * 
 * @version $Id$
 */
public class DefaultReferenceSpaceReferenceResolverTest
{
    private SpaceReferenceResolver<EntityReference> resolver;

    @Before
    public void setUp() throws Exception
    {
        this.resolver = new DefaultReferenceSpaceReferenceResolver();
        ReflectionUtils.setFieldValue(this.resolver, "entityReferenceResolver",
            new DefaultReferenceEntityReferenceResolver());
    }

    @Test
    public void testResolveWithExplicitSpaceReference()
    {
        SpaceReference reference = this.resolver.resolve(null, new SpaceReference("space", new WikiReference("wiki")));

        Assert.assertEquals("space", reference.getName());
        Assert.assertEquals("wiki", reference.getWikiReference().getName());
    }
}
