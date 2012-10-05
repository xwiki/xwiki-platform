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
package org.xwiki.uiextension.internal.filter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.uiextension.UIExtension;
import org.xwiki.uiextension.UIExtensionFilter;

/**
 * Sort a list of {@link UIExtension} by their IDs.
 *
 * @version $Id$
 * @since 4.3M1
 */
@Component
@Singleton
@Named("sortById")
public class SortByIdFilter implements UIExtensionFilter
{
    /**
     * Comparator comparing the IDs of two {@link UIExtension}s.
     */
    public class UIExtensionIdComparator implements Comparator<UIExtension>
    {
        @Override
        public int compare(UIExtension source, UIExtension target)
        {
            return source.getId().compareToIgnoreCase(target.getId());
        }
    }

    /**
     * @param extensions The list of {@link UIExtension}s to filter
     * @param ignored This filter doesn't require any parameter, passed arguments will be ignored
     * @return The filter list
     */
    @Override
    public List<UIExtension> filter(List<UIExtension> extensions, String... ignored)
    {
        List<UIExtension> results = new ArrayList<UIExtension>();
        results.addAll(extensions);
        Collections.sort(results, new UIExtensionIdComparator());

        return results;
    }
}
