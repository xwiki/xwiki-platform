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
 *
 */
package com.xpn.xwiki.plugin.charts.wizard;

import com.xpn.xwiki.plugin.charts.source.TableDataSource;

public class DatasourceDefaultsHelper
{
    public String getDefaultTableNumber()
    {
        return TableDataSource.DEFAULT_TABLE_NUMBER + "";
    }

    public String getDefaultRange()
    {
        return TableDataSource.DEFAULT_RANGE + "";
    }

    public String getDefaultHasHeaderRow()
    {
        return TableDataSource.DEFAULT_HAS_HEADER_ROW + "";
    }

    public String getDefaultHasHeaderColumn()
    {
        return TableDataSource.DEFAULT_HAS_HEADER_COLUMN + "";
    }

    public String getDefaultDecimalSymbol()
    {
        return TableDataSource.DEFAULT_DECIMAL_SYMBOL + "";
    }

    public String getDefaultIgnoreAlpha()
    {
        return TableDataSource.DEFAULT_IGNORE_ALPHA + "";
    }
}
