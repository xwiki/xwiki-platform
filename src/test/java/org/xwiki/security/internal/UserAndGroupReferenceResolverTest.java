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
 *
 */
package org.xwiki.security.internal;

import junit.framework.TestCase;

import org.xwiki.test.AbstractComponentTestCase;

import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.DocumentReference;

import static org.junit.Assert.*;
import org.junit.Test;

public class UserAndGroupReferenceResolverTest  extends AbstractComponentTestCase
{

    @Test
    public void testResolver()
    {
        try {
            DocumentReferenceResolver<String> resolver = getComponentManager().lookup(DocumentReferenceResolver.class, "user");
            DocumentReferenceResolver<String> defaultResolver = getComponentManager().lookup(DocumentReferenceResolver.class, "user");
            assertTrue(resolver.resolve("Bosse")                          .equals(defaultResolver.resolve("xwiki:XWiki.Bosse")));
            assertTrue(resolver.resolve("bossesSpace.Bosse")              .equals(defaultResolver.resolve("xwiki:bossesSpace.Bosse")));
            assertTrue(resolver.resolve("Bosse", "bossesWiki")            .equals(defaultResolver.resolve("bossesWiki:XWiki.Bosse")));
            assertTrue(resolver.resolve("bossesSpace.Bosse", "bossesWiki").equals(defaultResolver.resolve("bossesWiki:bossesSpace.Bosse")));
            assertTrue(resolver.resolve("bossesWiki:bossesSpace.Bosse")   .equals(defaultResolver.resolve("bossesWiki:bossesSpace.Bosse")));
        } catch(Exception e) {
            System.out.println("Caught exception: " + e);
            e.printStackTrace(System.out);
            assert false;
        }


    }
}
