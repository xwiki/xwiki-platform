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
package org.xwiki.gwt.wysiwyg.client.plugin.font;

import java.util.HashMap;
import java.util.Map;

/**
 * Caches the index of selected values.
 * 
 * @version $Id$
 */
public class CachedListBoxPicker extends DynamicListBoxPicker
{
    /**
     * The cache maps values to their index in the list.
     */
    private Map<String, Integer> cache = new HashMap<String, Integer>();

    /**
     * {@inheritDoc}
     * 
     * @see DynamicListBoxPicker#setValue(int, String)
     */
    public void setValue(int index, String value)
    {
        cache.clear();
        super.setValue(index, value);
    }

    /**
     * {@inheritDoc}
     * 
     * @see DynamicListBoxPicker#setSelectedValue(String, Matcher)
     */
    protected void setSelectedValue(String value, Matcher<String> matcher)
    {
        Integer index = cache.get(value);
        if (index != null) {
            setSelectedIndex(index);
        } else {
            super.setSelectedValue(value, matcher);
            cache.put(value, getSelectedIndex());
        }
    }
}
