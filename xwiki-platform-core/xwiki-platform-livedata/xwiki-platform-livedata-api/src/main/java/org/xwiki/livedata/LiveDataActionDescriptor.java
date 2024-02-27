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
package org.xwiki.livedata;

import java.util.HashMap;
import java.util.Map;

import org.xwiki.stability.Unstable;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Describes a live data action.
 *
 * @version $Id$
 * @since 12.10.1
 * @since 13.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LiveDataActionDescriptor extends BaseDescriptor
{
    /**
     * The action pretty name.
     */
    private String name;

    /**
     * The action description.
     */
    private String description;

    /**
     * Specifies the action icon. The map contains meta data about the icon, such as the icon set name, icon set type,
     * URL or CSS class name.
     */
    private Map<String, Object> icon;

    /**
     * Specifies the live data property that can be used to asses whether the current user is allowed to perform this
     * action on a given entry.
     */
    private String allowProperty;

    /**
     * Specifies the live data property that holds the URL that can be used to perform this action on a given entry.
     */
    private String urlProperty;

    /**
     * Async is a boolean object so that the field is not serialized when not explicitly defined. A {@code null} value
     * is equivalent to a {@code false} value.
     */
    private LiveDataAsyncActionDescriptor async;

    

    /**
     * Default constructor.
     */
    public LiveDataActionDescriptor()
    {
    }

    /**
     * Creates a new descriptor for the specified action.
     *
     * @param id the action id
     */
    public LiveDataActionDescriptor(String id)
    {
        this.setId(id);
    }

    /**
     * @return the action pretty name
     */
    public String getName()
    {
        return name;
    }

    /**
     * Sets the action pretty name.
     *
     * @param name the new pretty name
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * @return the action description
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * Sets the action description.
     *
     * @param description the new action description
     */
    public void setDescription(String description)
    {
        this.description = description;
    }

    /**
     * @return the icon meta data
     */
    public Map<String, Object> getIcon()
    {
        return icon;
    }

    /**
     * Sets the icon meta data.
     *
     * @param icon the new icon meta data
     */
    public void setIcon(Map<String, Object> icon)
    {
        this.icon = icon;
    }

    /**
     * @return the live data property used to determine if the current user is allowed to perform this action on a given
     *     live data entry
     * @since 12.10.4
     * @since 13.0
     */
    public String getAllowProperty()
    {
        return allowProperty;
    }

    /**
     * Sets the live data property to be used to determine if the current user is allowed to perform this action on a
     * given live data entry.
     *
     * @param allowProperty the live data property used to determine if the current user is allowed to perform this
     *     action on a given live data entry
     * @since 12.10.4
     * @since 13.0
     */
    public void setAllowProperty(String allowProperty)
    {
        this.allowProperty = allowProperty;
    }

    /**
     * @return the live data property that holds the URL that can be used to perform this action on a given entry
     * @since 12.10.4
     * @since 13.0
     */
    public String getUrlProperty()
    {
        return urlProperty;
    }

    /**
     * Sets the live data property that holds the URL that can be used to perform this action on a given entry.
     *
     * @param urlProperty the live data property that holds the URL that can be used to perform this action on a
     *     given entry
     * @since 12.10.4
     * @since 13.0
     */
    public void setUrlProperty(String urlProperty)
    {
        this.urlProperty = urlProperty;
    }

    /**
     * Prevent {@code null} values where it's possible.
     */
    public void initialize()
    {
        if (this.icon == null) {
            this.icon = new HashMap<>();
        }
    }

    /**
     * @return the descriptor for handling this action asynchronously
     * @since 16.2.0RC1
     */
    @Unstable
    public LiveDataAsyncActionDescriptor getAsync()
    {
        return this.async;
    }

    /**
     * @param async the descriptor for handling this action asynchronously
     * @since 16.2.0RC1
     */
    @Unstable
    public void setAsync(LiveDataAsyncActionDescriptor async)
    {
        this.async = async;
    }
}
