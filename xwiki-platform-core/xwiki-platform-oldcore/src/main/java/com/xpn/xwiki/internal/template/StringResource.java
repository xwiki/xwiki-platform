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
package com.xpn.xwiki.internal.template;

import java.util.Map;

import org.apache.commons.lang3.NotImplementedException;
import org.xwiki.filter.input.StringInputSource;

import com.xpn.xwiki.internal.skin.AbstractInputSourceResource;

/**
 * @version $Id$
 * @since 9.6RC1
 */
public class StringResource extends AbstractInputSourceResource<StringInputSource>
{
    /**
     * @param resourceContent the content of the resource
     */
    public StringResource(String resourceContent)
    {
        super(null, "StringResource", null, new StringInputSource(resourceContent));
    }

    @Override
    public String getURL(boolean forceSkinAction) throws Exception
    {
        // A StringResource does not depends on a particular URL
        throw new NotImplementedException("Method not implemented");
    }

    @Override
    public String getURL(boolean forceSkinAction, Map<String, String> queryParameters) throws Exception
    {
        return getURL(forceSkinAction);
    }
}
