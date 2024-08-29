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
package org.xwiki.rendering.wikimacro.internal;

import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;

import com.xpn.xwiki.objects.BaseObject;

import static org.xwiki.rendering.wikimacro.internal.LegacyWikiMacroConstants.MACRO_DEFAULT_CATEGORY_PROPERTY;

/**
 * Legacy aspect for {@link DefaultWikiMacroFactory}.
 *
 * @version $Id$
 * @since 14.6RC1
 */
public aspect DefaultWikiMacroFactoryAspect
{
    private pointcut getDefaultCategoriesPC(BaseObject macroDefinition):
        call(* org.xwiki.rendering.wikimacro.internal.DefaultWikiMacroFactory.getDefaultCategories(..))
            && args(macroDefinition);

    Object around(BaseObject macroDefinition): getDefaultCategoriesPC(macroDefinition) {
        Set<String> defaultCategories = (Set<String>) proceed(macroDefinition);
        if (CollectionUtils.isEmpty(defaultCategories)) {
            String stringValue = macroDefinition.getStringValue(MACRO_DEFAULT_CATEGORY_PROPERTY);
            if (stringValue != null) {
                defaultCategories = Set.of(stringValue);
            } else {
                defaultCategories = Set.of();
            }
        }
        return defaultCategories;
    }
}
