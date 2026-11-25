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
package org.xwiki.livedata.internal.livetable;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.xwiki.livedata.LiveDataQuery;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test of {@link LiveTableLiveDataSourceContentTrust}.
 *
 * @version $Id$
 */
@ComponentTest
class LiveTableLiveDataSourceContentTrustTest
{
    @InjectMockComponents
    private LiveTableLiveDataSourceContentTrust contentTrust;

    @Test
    void isContentTrustedWithStandardParameters()
    {
        LiveDataQuery.Source source = new LiveDataQuery.Source("liveTable");
        source.setParameter("className", "Space.Class");
        source.setParameter("translationPrefix", "space.class.");
        source.setParameter("$doc", "Space.Page");

        assertTrue(this.contentTrust.isContentTrusted(source));
    }

    @Test
    void isContentTrustedWithoutParameters()
    {
        assertTrue(this.contentTrust.isContentTrusted(new LiveDataQuery.Source("liveTable")));
    }

    @ParameterizedTest
    @ValueSource(strings = { "resultPage", "template" })
    void isContentTrustedWithCustomEntriesOrigin(String parameter)
    {
        LiveDataQuery.Source source = new LiveDataQuery.Source("liveTable");
        source.setParameter("className", "Space.Class");
        // The value doesn't matter: an empty one still changes where the entries are read from.
        source.setParameter(parameter, "");

        assertFalse(this.contentTrust.isContentTrusted(source));
    }
}
