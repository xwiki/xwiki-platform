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
package org.xwiki.rendering.async.internal.service;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.xwiki.resource.CreateResourceReferenceException;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.url.ExtendedURL;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Validate {@link AsyncRendererResourceReferenceResolver}.
 * 
 * @version $Id$
 */
@ComponentTest
public class AsyncRendererResourceReferenceResolverTest
{
    @InjectMockComponents
    private AsyncRendererResourceReferenceResolver resolver;

    @Test
    void resolve() throws MalformedURLException, CreateResourceReferenceException, UnsupportedResourceReferenceException
    {
        String prefix = "xwiki/" + AsyncRendererResourceReferenceHandler.HINT;

        assertEquals(
            new AsyncRendererResourceReference(AsyncRendererResourceReferenceHandler.TYPE, Arrays.asList("id1", "id2"),
                "myclientId", 42, "mywiki"),
            this.resolver.resolve(new ExtendedURL(
                new URL("http://host/xwiki/asyncrenderer/id1/id2?clientId=myclientId&wiki=mywiki&timeout=42"), prefix),
                AsyncRendererResourceReferenceHandler.TYPE, null));

        assertEquals(
            new AsyncRendererResourceReference(AsyncRendererResourceReferenceHandler.TYPE, Arrays.asList(), null,
                Long.MAX_VALUE, null),
            this.resolver.resolve(new ExtendedURL(new URL("http://host/xwiki/asyncrenderer/"), prefix),
                AsyncRendererResourceReferenceHandler.TYPE, null));
    }
}
