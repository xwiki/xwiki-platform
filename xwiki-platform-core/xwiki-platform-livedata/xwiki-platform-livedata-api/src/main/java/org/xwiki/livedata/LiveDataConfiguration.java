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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Describes the live data configuration.
 * 
 * @version $Id$
 * @since 12.10
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class LiveDataConfiguration implements InitializableLiveDataElement
{
    private String id;

    private LiveDataQuery query;

    private LiveData data;

    private LiveDataMeta meta;

    /**
     * @return the live data instance id
     */
    public String getId()
    {
        return id;
    }

    /**
     * Set the live data instance id.
     * 
     * @param id the new live data instance id
     */
    public void setId(String id)
    {
        this.id = id;
    }

    /**
     * @return the query used to retrieve the live data
     */
    public LiveDataQuery getQuery()
    {
        return query;
    }

    /**
     * Set the query used to retrieve the live data.
     * 
     * @param query the new query
     */
    public void setQuery(LiveDataQuery query)
    {
        this.query = query;
    }

    /**
     * @return the live data
     */
    public LiveData getData()
    {
        return data;
    }

    /**
     * Sets the live data.
     * 
     * @param data the new live data
     */
    public void setData(LiveData data)
    {
        this.data = data;
    }

    /**
     * @return the configuration used to display the live data
     */
    public LiveDataMeta getMeta()
    {
        return meta;
    }

    /**
     * Set the configuration used to display the live data.
     * 
     * @param meta the new meta configuration
     */
    public void setMeta(LiveDataMeta meta)
    {
        this.meta = meta;
    }

    @Override
    public void initialize()
    {
        if (this.query == null) {
            this.query = new LiveDataQuery();
        }
        this.query.initialize();

        if (this.data == null) {
            this.data = new LiveData();
        }

        if (this.meta == null) {
            this.meta = new LiveDataMeta();
        }
        this.meta.initialize();
    }
}
