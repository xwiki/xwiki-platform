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
package com.xpn.xwiki.objects;

import org.xwiki.stability.Unstable;
import org.xwiki.store.merge.MergeManagerResult;

import com.xpn.xwiki.doc.merge.MergeConfiguration;

/**
 * Property defining a string stored in a text field to override the limitation of 255 characters.
 *
 * @version $Id$
 */
public class LargeStringProperty extends BaseStringProperty
{
    /**
     * The type used as a hint to find the property.
     * @since 18.2.0RC1
     */
    @Unstable
    public static final String PROPERTY_TYPE = "LargeString";

    private static final long serialVersionUID = 1L;

    @Override
    protected MergeManagerResult<Object, Object> mergeValue(Object previousValue, Object newValue,
        MergeConfiguration configuration)
    {
        MergeManagerResult<String, String> valueMergeManagerResult = getMergeManager()
            .mergeLines((String) previousValue, (String) newValue, getValue(), configuration);

        MergeManagerResult<Object, Object> result = new MergeManagerResult<>();
        result.setLog(valueMergeManagerResult.getLog());
        result.setMergeResult(valueMergeManagerResult.getMergeResult());
        // We cannot convert a Conflict<String> to Conflict<Object> right now, so we're loosing conflicts info here...
        result.setModified(valueMergeManagerResult.isModified());
        return result;
    }

    @Override
    public String getPropertyType()
    {
        return PROPERTY_TYPE;
    }
}
