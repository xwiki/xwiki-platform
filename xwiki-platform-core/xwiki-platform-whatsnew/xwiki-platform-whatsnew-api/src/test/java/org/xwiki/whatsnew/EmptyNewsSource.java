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
package org.xwiki.whatsnew;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.xwiki.extension.version.Version;
import org.xwiki.user.UserReference;

/**
 * Empty News source for the tests.
 *
 * @version $Id$
 */
public class EmptyNewsSource implements NewsSource
{
    @Override
    public NewsSource forUser(UserReference userReference)
    {
        return this;
    }

    @Override
    public NewsSource forCategories(Set<NewsCategory> wantedCategories)
    {
        return this;
    }

    @Override
    public NewsSource forXWikiVersion(Version targetXWikiVersion)
    {
        return this;
    }

    @Override
    public NewsSource forExtraParameters(Map<String, Object> extraParameters)
    {
        return this;
    }

    @Override
    public NewsSource withCount(int count)
    {
        return this;
    }

    @Override
    public List<NewsSourceItem> build()
    {
        return Collections.emptyList();
    }
}
