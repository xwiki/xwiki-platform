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
package org.xwiki.vfs.internal;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.url.ExtendedURL;
import org.xwiki.vfs.VfsResourceReference;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link VfsResourceReferenceResolver}.
 *
 * @version $Id$
 * @since 7.4M2
 */
public class VfsResourceReferenceResolverTest
{
    @Rule
    public MockitoComponentMockingRule<VfsResourceReferenceResolver> mocker =
        new MockitoComponentMockingRule<>(VfsResourceReferenceResolver.class);

    @Test
    public void resolve() throws Exception
    {
        ExtendedURL extendedURL = new ExtendedURL(
            Arrays.asList("attach:wiki:space.page@attachment", "path1", "path2", "test.txt"),
            Collections.singletonMap("key", Arrays.asList("value")));

        VfsResourceReference reference = this.mocker.getComponentUnderTest().resolve(extendedURL,
            VfsResourceReference.TYPE, Collections.<String, Object>emptyMap());

        VfsResourceReference expected = new VfsResourceReference(URI.create("attach:wiki:space.page@attachment"),
            "path1/path2/test.txt");
        expected.addParameter("key", "value");
        assertEquals(expected, reference);
    }
}