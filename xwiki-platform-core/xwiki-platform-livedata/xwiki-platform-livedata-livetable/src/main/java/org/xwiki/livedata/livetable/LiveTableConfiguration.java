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
package org.xwiki.livedata.livetable;

import java.util.List;
import java.util.Map;

/**
 * Represents the configuration passed when creating a live table instance.
 * 
 * @version $Id$
 * @since 12.10
 */
public class LiveTableConfiguration
{
    private String id;

    private List<String> columns;

    private Map<String, Object> columnProperties;

    private Map<String, Object> options;

    /**
     * Default constructor.
     */
    public LiveTableConfiguration()
    {
    }

    /**
     * Creates a new instance.
     * 
     * @param id the live table id
     * @param columns the list of live table columns
     * @param columnProperties the column properties
     * @param options the live table options
     */
    public LiveTableConfiguration(String id, List<String> columns, Map<String, Object> columnProperties,
        Map<String, Object> options)
    {
        this.id = id;
        this.columns = columns;
        this.columnProperties = columnProperties;
        this.options = options;
    }

    /**
     * @return the live table id
     */
    public String getId()
    {
        return id;
    }

    /**
     * Sets the live table id.
     * 
     * @param id the new live table id
     */
    public void setId(String id)
    {
        this.id = id;
    }

    /**
     * @return the list of live table columns
     */
    public List<String> getColumns()
    {
        return columns;
    }

    /**
     * Sets the list of live table columns.
     * 
     * @param columns the new list of live table columns
     */
    public void setColumns(List<String> columns)
    {
        this.columns = columns;
    }

    /**
     * @return the live table column properties
     */
    public Map<String, Object> getColumnProperties()
    {
        return columnProperties;
    }

    /**
     * Sets the live table column properties.
     * 
     * @param columnProperties the new live table column properties
     */
    public void setColumnProperties(Map<String, Object> columnProperties)
    {
        this.columnProperties = columnProperties;
    }

    /**
     * @return the live table options
     */
    public Map<String, Object> getOptions()
    {
        return options;
    }

    /**
     * Sets the live table options.
     * 
     * @param options the new live table options
     */
    public void setOptions(Map<String, Object> options)
    {
        this.options = options;
    }
}
