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
package org.xwiki.whatsnew.internal.configured;

import java.sql.Date;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.xwiki.whatsnew.NewsSource;
import org.xwiki.whatsnew.NewsSourceItem;
import org.xwiki.whatsnew.internal.DefaultNewsSourceItem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CompositeNewsSource}.
 *
 * @version $Id$
 */
class CompositeNewsSourceTest
{
    @Test
    void buildWithOrder() throws Exception
    {
        DefaultNewsSourceItem item1 = new DefaultNewsSourceItem();
        item1.setTitle(Optional.of("item1"));
        item1.setPublishedDate(Optional.of(Date.from(Instant.now().minusSeconds(60))));

        NewsSource source1 = mock(NewsSource.class, "source1");
        when(source1.build()).thenReturn(List.of(item1));

        DefaultNewsSourceItem item2 = new DefaultNewsSourceItem();
        item2.setTitle(Optional.of("item2"));
        item2.setPublishedDate(Optional.of(Date.from(Instant.now())));

        NewsSource source2 = mock(NewsSource.class, "source2");
        when(source2.build()).thenReturn(List.of(item2));

        CompositeNewsSource compositeNewsSource = new CompositeNewsSource(List.of(source1, source2));
        List<NewsSourceItem> items =  compositeNewsSource.build();

        // We verify that we sort descending, i.e. the first item from source1 is newer than the second one from
        // source2.
        assertEquals("item2", items.get(0).getTitle().get());
    }
}
