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
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.uiextension.UIExtension;
import org.xwiki.uiextension.UIExtensionFilter;

/**
 * Exclude {@link UIExtension}s from a list, identifying them by their ID.
 *
 * @version $Id$
 * @since 4.3M1
 */
@Component
@Singleton
@Named("exclude")
public class ExcludeFilter implements UIExtensionFilter
{
    @Override
    public List<UIExtension> filter(List<UIExtension> extensions, String... ids)
    {
        List<String> extensionIds = Arrays.asList(ids);
        List<UIExtension> results = new ArrayList<>();

        for (UIExtension extension : extensions) {
            if (!extensionIds.contains(extension.getId())) {
                results.add(extension);
            }
        }

        return results;
    }
}
