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
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.internal.MapCache;
import org.xwiki.component.descriptor.ComponentRole;
import org.xwiki.component.descriptor.DefaultComponentRole;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.rendering.async.internal.DefaultAsyncContext.RightEntry;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
class AsyncRendererCacheTest
{
    @MockComponent
    private CacheManager cacheManager;

    @MockComponent
    private AuthorizationManager authorization;

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

    private void setRoles(ComponentRole<?>... roles)
    {
        this.status.setRoles(new HashSet<>(Arrays.asList(roles)));
    }

    private void setReferences(EntityReference... references)
    {
        this.status.setReferences(new HashSet<>(Arrays.asList(references)));
    }

    private void setRights(RightEntry... rights)
    {
        for (RightEntry right : rights) {
            when(this.authorization.hasAccess(right.getRight(), right.getUserReference(), right.getEntityReference()))
                .thenReturn(right.isAllowed());
        }

        this.status.setRights(Set.of(rights));
    }

    private List<String> getId()
    {
        return this.status.getRequest().getId();
    }

    // Tests

    @Test
    void invalidateSyncOnComponentType()
    {
        setRoleTypes(String.class);

        this.asyncCache.put(this.status);

        assertSame(this.status, this.asyncCache.getSync(getId()));

        this.asyncCache.cleanCache(Integer.class, "hint");

        assertSame(this.status, this.asyncCache.getSync(getId()));

        this.asyncCache.cleanCache(String.class, "hint");

        assertNull(this.asyncCache.getSync(getId()));
    }

    @Test
    void invalidateSyncOnComponent()
    {
        setRoles(new DefaultComponentRole<>(String.class, "hint"));

        this.asyncCache.put(this.status);

        this.asyncCache.cleanCache(Integer.class, "hint");

        assertSame(this.status, this.asyncCache.getSync(getId()));

        this.asyncCache.cleanCache(String.class, "otherhint");

        assertSame(this.status, this.asyncCache.getSync(getId()));

        this.asyncCache.cleanCache(String.class, "hint");

        assertNull(this.asyncCache.getSync(getId()));
    }

    @Test
    void invalidateSyncOnSameReference()
    {
        setReferences(new ObjectReference("name", new DocumentReference("wiki", "Space", "Document")));

        this.asyncCache.put(this.status);

        this.asyncCache.cleanCache(new ObjectReference("other", new DocumentReference("wiki", "Space", "Document")));

        assertSame(this.status, this.asyncCache.getSync(getId()));

        this.asyncCache.cleanCache(new DocumentReference("wiki", "Space", "Document"));

        assertSame(this.status, this.asyncCache.getSync(getId()));

        this.asyncCache.cleanCache(new ObjectReference("name", new DocumentReference("wiki", "Space", "Document")));

        assertNull(this.asyncCache.getSync(getId()));
    }

    @Test
    void invalidateSyncOnChildReference()
    {
        setReferences(new DocumentReference("wiki", "Space", "Document"));

        this.asyncCache.put(this.status);

        assertSame(this.status, this.asyncCache.getSync(getId()));

        this.asyncCache.cleanCache(new ObjectReference("name", new DocumentReference("wiki", "Space", "Document")));

        assertNull(this.asyncCache.getSync(getId()));
    }

    @Test
    void invalidateSyncOnWiki()
    {
        setReferences(new DocumentReference("wiki", "Space", "Document"));

        this.asyncCache.put(this.status);

        assertSame(this.status, this.asyncCache.getSync(getId()));

        this.asyncCache.cleanCache("otherwiki");

        assertSame(this.status, this.asyncCache.getSync(getId()));

        this.asyncCache.cleanCache("wiki");

        assertNull(this.asyncCache.getSync(getId()));
    }

    @Test
    void invalidateSyncOnRight() throws IllegalAccessException
    {
        DocumentReference document = new DocumentReference("wiki", "Space", "Document");
        DocumentReference user = new DocumentReference("wiki", "XWiki", "User");
        setRights(new RightEntry(Right.VIEW, document, user, true));
        Map<RightEntry, Set<String>> rightMapping =
            (Map<RightEntry, Set<String>>) FieldUtils.readField(this.asyncCache, "rightMapping", true);

        this.asyncCache.put(this.status);

        assertSame(this.status, this.asyncCache.getSync(getId()));
        assertEquals(1, rightMapping.size());

        this.asyncCache.cleanCacheForRight();

        assertSame(this.status, this.asyncCache.getSync(getId()));

        when(this.authorization.hasAccess(Right.VIEW, document, user)).thenReturn(false);

        this.asyncCache.cleanCacheForRight();

        assertNull(this.asyncCache.getSync(getId()));
        assertEquals(0, rightMapping.size());
    }

    @Test
    void getAsyncSingleClient()
    {
        this.status.addClient("42");

        this.asyncCache.put(this.status);

        assertSame(this.status, this.asyncCache.getAsync("42"));

        assertNull(this.asyncCache.getAsync("42"));
    }

    @Test
    void getAsyncSeveralClients()
    {
        this.status.addClient("1");
        this.status.addClient("2");

        this.asyncCache.put(this.status);

        assertSame(this.status, this.asyncCache.getAsync("1"));

        assertNull(this.asyncCache.getAsync("1"));

        assertSame(this.status, this.asyncCache.getAsync("2"));

        assertNull(this.asyncCache.getAsync("2"));
    }
}
