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
package org.xwiki.icon.internal;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.icon.IconException;
import org.xwiki.icon.IconSet;
import org.xwiki.icon.IconSetManager;
import org.xwiki.icon.internal.context.IconContextStore;
import org.xwiki.icon.internal.context.IconSetContext;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Validate {@link IconContextStore}.
 * 
 * @version $Id$
 */
@ComponentTest
public class IconContextStoreTest
{
    private static final String ICONSET_NAME = "iconset_name";

    @InjectMockComponents
    private IconContextStore store;

    @MockComponent
    private IconSetManager manager;

    @MockComponent
    private IconSetContext context;

    private IconSet iconSet = mock(IconSet.class);

    @BeforeEach
    public void beforeEach()
    {
        when(this.iconSet.getName()).thenReturn(ICONSET_NAME);
    }

    @Test
    public void saveNoEntry()
    {
        Map<String, Serializable> contextStore = new HashMap<>();

        this.store.save(contextStore, Collections.emptySet());

        assertTrue(contextStore.isEmpty());
    }

    @Test
    public void save() throws IconException
    {
        Map<String, Serializable> contextStore = new HashMap<>();

        this.store.save(contextStore, Collections.singleton(IconContextStore.PROP_ICON_THEME));

        assertTrue(contextStore.isEmpty());

        when(this.manager.getCurrentIconSet()).thenReturn(this.iconSet);

        this.store.save(contextStore, Collections.singleton(IconContextStore.PROP_ICON_THEME));

        assertEquals(ICONSET_NAME, contextStore.get(IconContextStore.PROP_ICON_THEME));
    }

    @Test
    public void restore() throws IconException
    {
        Map<String, Serializable> contextStore = new HashMap<>();

        this.store.restore(contextStore);

        verifyNoMoreInteractions(this.context);

        contextStore.put(IconContextStore.PROP_ICON_THEME, ICONSET_NAME);

        this.store.restore(contextStore);

        verify(this.manager).getIconSet(ICONSET_NAME);
        verify(this.context).setIconSet(null);

        when(this.manager.getIconSet(ICONSET_NAME)).thenReturn(this.iconSet);

        this.store.restore(contextStore);

        verify(this.context).setIconSet(same(this.iconSet));
    }
}
