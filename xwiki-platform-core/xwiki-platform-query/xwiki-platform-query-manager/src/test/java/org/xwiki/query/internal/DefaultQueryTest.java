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
package org.xwiki.query.internal;

import org.junit.jupiter.api.Test;
import org.xwiki.query.Query;
import org.xwiki.query.QueryFilter;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link DefaultQuery}.
 *
 * @version $Id$
 */
public class DefaultQueryTest
{
    @Test
    public void addSameFilterMultipleTimes()
    {
        Query query = new DefaultQuery("", Query.XWQL, null);
        QueryFilter filter = new HiddenDocumentFilter();

        query.addFilter(filter);
        query.addFilter(filter);

        // We're assuming filters are used with our Component manager, with @Singleton. To go further we'd need to add
        // QueryFilter#getName() or QueryFilter#getID() in order to forbid the addition of multiple identical filters.
        assertTrue(query.getFilters().size() == 1);
    }
}
