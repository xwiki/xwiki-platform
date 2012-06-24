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

import org.junit.Test;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.AbstractComponentTestCase;

import junit.framework.Assert;

public class UserAndGroupReferenceResolverTest  extends AbstractComponentTestCase
{

    @Test
    public void testResolver()
    {

        try {
            DocumentReferenceResolver<String> resolver = getComponentManager().getInstance(
                DocumentReferenceResolver.TYPE_STRING, "user");
            DocumentReferenceResolver<String> defaultResolver = getComponentManager().getInstance(
                DocumentReferenceResolver.TYPE_STRING);
            Assert.assertTrue(resolver.resolve("Bosse").equals(defaultResolver.resolve("xwiki:XWiki.Bosse")));
            Assert.assertTrue(
                resolver.resolve("bossesSpace.Bosse").equals(defaultResolver.resolve("xwiki:bossesSpace.Bosse")));
            Assert.assertTrue(resolver.resolve("Bosse", new WikiReference("bossesWiki"))
                .equals(defaultResolver.resolve("bossesWiki:XWiki.Bosse")));
            Assert.assertTrue(resolver.resolve("bossesSpace.Bosse", new WikiReference("bossesWiki"))
                .equals(defaultResolver.resolve("bossesWiki:bossesSpace.Bosse")));
            Assert.assertTrue(resolver.resolve("bossesWiki:bossesSpace.Bosse")
                .equals(defaultResolver.resolve("bossesWiki:bossesSpace.Bosse")));
        } catch(Exception e) {
            System.out.println("Caught exception: " + e);
            e.printStackTrace(System.out);
            assert false;
        }
    }
}
