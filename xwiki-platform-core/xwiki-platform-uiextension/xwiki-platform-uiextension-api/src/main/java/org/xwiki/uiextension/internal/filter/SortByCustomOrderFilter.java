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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.uiextension.UIExtension;
import org.xwiki.uiextension.UIExtensionFilter;

/**
 * Sort a list of {@link UIExtension} by following a custom order strategy. If some extension IDs are not passed
 * through {@link #filter(java.util.List, String...)} they will be put at the end of the list.
 *
 * @version $Id$
 * @since 4.3M1
 */
@Component
@Named("sortByCustomOrder")
@Singleton
public class SortByCustomOrderFilter implements UIExtensionFilter
{
    /**
     * @param extensions The list of {@link UIExtension}s to filter
     * @param ids The IDs of {@link UIExtension} to display first, in the desired order
     * @return The filtered list
     */
    @Override
    public List<UIExtension> filter(List<UIExtension> extensions, String... ids)
    {
        List<String> extensionIds = Arrays.asList(ids);
        Collections.reverse(extensionIds);
        List<UIExtension> results = new ArrayList<>();
        results.addAll(extensions);

        for (String id : extensionIds) {
            for (UIExtension extension : results) {
                if (id.equals(extension.getId())) {
                    results.remove(extension);
                    results.add(0, extension);
                    break;
                }
            }
        }
        return results;
    }
}
