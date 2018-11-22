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
package org.xwiki.rendering.async.internal;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.internal.MapCache;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Validate {@link AsyncRendererCache}.
 * 
 * @version $Id$
 */
@ComponentTest
public class AsyncRendererCacheTest
{
    @MockComponent
    private CacheManager cacheManager;

    @InjectMockComponents
    private AsyncRendererCache asyncCache;

    private AsyncRendererJobStatus status;

    private AsyncRenderer renderer;

    @BeforeComponent
    public void beforeComponent() throws CacheException
    {
        when(this.cacheManager.<AsyncRendererJobStatus>createNewCache(any())).thenReturn(new MapCache<>());
    }

    @BeforeEach
    public void beforeEach()
    {
        this.renderer = mock(AsyncRenderer.class);

        when(this.renderer.isAsyncAllowed()).thenReturn(true);
        when(this.renderer.isCacheAllowed()).thenReturn(true);

        AsyncRendererJobRequest request = new AsyncRendererJobRequest();
        request.setId(Arrays.asList("entry", "id"));
        request.setRenderer(this.renderer);
        AsyncRendererResult result = new AsyncRendererResult("result");

        this.status = new AsyncRendererJobStatus(request, result);
    }

    private void setRoleTypes(Type... types)
    {
        this.status.setRoleTypes(new HashSet<>(Arrays.asList(types)));
    }

    private List<String> getId()
    {
        return this.status.getRequest().getId();
    }

    // Tests

    @Test
    public void invalidateOnComponent()
    {
        setRoleTypes(String.class);

        this.asyncCache.put(this.status);

        assertSame(this.status, this.asyncCache.get(getId()));

        this.asyncCache.cleanCache(String.class, "hint");

        assertNull(this.asyncCache.get(getId()));
    }

    @Test
    public void noinvalidateOnDifferentComponent()
    {
        setRoleTypes(String.class);

        this.asyncCache.put(this.status);

        assertSame(this.status, this.asyncCache.get(getId()));

        this.asyncCache.cleanCache(Integer.class, "hint");

        assertSame(this.status, this.asyncCache.get(getId()));
    }
}
