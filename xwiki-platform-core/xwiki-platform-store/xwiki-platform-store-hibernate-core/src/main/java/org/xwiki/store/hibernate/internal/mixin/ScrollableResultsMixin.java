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
package org.xwiki.store.hibernate.internal.mixin;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Blob;
import java.sql.Clob;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.hibernate.ScrollableResults;
import org.xwiki.store.hibernate.internal.patcher.BinaryOnly;
import org.xwiki.store.hibernate.internal.patcher.Mixin;

/**
 * Restore the Hibernate 5 methods of {@link ScrollableResults}, which used to always expose the current row as an
 * array (even when the query selects a single item).
 *
 * @param <R> the type of the results
 * @version $Id$
 */
@Mixin
public interface ScrollableResultsMixin<R> extends ScrollableResults<R>
{
    private static Object[] toRow(Object result)
    {
        return result instanceof Object[] row ? row : new Object[] { result };
    }

    /**
     * Hibernate 5 {@code get()} returned the current row as an array.
     *
     * @return the current row
     */
    @BinaryOnly("get")
    default Object[] getRow()
    {
        return toRow(get());
    }

    /**
     * Hibernate 5 {@code get(int)}.
     *
     * @param col the index of the column, starting from 0
     * @return the value of the column in the current row
     */
    @BinaryOnly("get")
    default Object getColumn(int col)
    {
        return toRow(get())[col];
    }

    /**
     * Hibernate 5 {@code getBigDecimal}.
     *
     * @param col the index of the column, starting from 0
     * @return the value of the column in the current row
     */
    @BinaryOnly("getBigDecimal")
    default BigDecimal getBigDecimalColumn(int col)
    {
        return (BigDecimal) toRow(get())[col];
    }

    /**
     * Hibernate 5 {@code getBigInteger}.
     *
     * @param col the index of the column, starting from 0
     * @return the value of the column in the current row
     */
    @BinaryOnly("getBigInteger")
    default BigInteger getBigIntegerColumn(int col)
    {
        return (BigInteger) toRow(get())[col];
    }

    /**
     * Hibernate 5 {@code getBinary}.
     *
     * @param col the index of the column, starting from 0
     * @return the value of the column in the current row
     */
    @BinaryOnly("getBinary")
    default byte[] getBinaryColumn(int col)
    {
        return (byte[]) toRow(get())[col];
    }

    /**
     * Hibernate 5 {@code getBlob}.
     *
     * @param col the index of the column, starting from 0
     * @return the value of the column in the current row
     */
    @BinaryOnly("getBlob")
    default Blob getBlobColumn(int col)
    {
        return (Blob) toRow(get())[col];
    }

    /**
     * Hibernate 5 {@code getBoolean}.
     *
     * @param col the index of the column, starting from 0
     * @return the value of the column in the current row
     */
    @BinaryOnly("getBoolean")
    default Boolean getBooleanColumn(int col)
    {
        return (Boolean) toRow(get())[col];
    }

    /**
     * Hibernate 5 {@code getByte}.
     *
     * @param col the index of the column, starting from 0
     * @return the value of the column in the current row
     */
    @BinaryOnly("getByte")
    default Byte getByteColumn(int col)
    {
        return (Byte) toRow(get())[col];
    }

    /**
     * Hibernate 5 {@code getCalendar}.
     *
     * @param col the index of the column, starting from 0
     * @return the value of the column in the current row
     */
    @BinaryOnly("getCalendar")
    default Calendar getCalendarColumn(int col)
    {
        return (Calendar) toRow(get())[col];
    }

    /**
     * Hibernate 5 {@code getCharacter}.
     *
     * @param col the index of the column, starting from 0
     * @return the value of the column in the current row
     */
    @BinaryOnly("getCharacter")
    default Character getCharacterColumn(int col)
    {
        return (Character) toRow(get())[col];
    }

    /**
     * Hibernate 5 {@code getClob}.
     *
     * @param col the index of the column, starting from 0
     * @return the value of the column in the current row
     */
    @BinaryOnly("getClob")
    default Clob getClobColumn(int col)
    {
        return (Clob) toRow(get())[col];
    }

    /**
     * Hibernate 5 {@code getDate}.
     *
     * @param col the index of the column, starting from 0
     * @return the value of the column in the current row
     */
    @BinaryOnly("getDate")
    default Date getDateColumn(int col)
    {
        return (Date) toRow(get())[col];
    }

    /**
     * Hibernate 5 {@code getDouble}.
     *
     * @param col the index of the column, starting from 0
     * @return the value of the column in the current row
     */
    @BinaryOnly("getDouble")
    default Double getDoubleColumn(int col)
    {
        return (Double) toRow(get())[col];
    }

    /**
     * Hibernate 5 {@code getFloat}.
     *
     * @param col the index of the column, starting from 0
     * @return the value of the column in the current row
     */
    @BinaryOnly("getFloat")
    default Float getFloatColumn(int col)
    {
        return (Float) toRow(get())[col];
    }

    /**
     * Hibernate 5 {@code getInteger}.
     *
     * @param col the index of the column, starting from 0
     * @return the value of the column in the current row
     */
    @BinaryOnly("getInteger")
    default Integer getIntegerColumn(int col)
    {
        return (Integer) toRow(get())[col];
    }

    /**
     * Hibernate 5 {@code getLocale}.
     *
     * @param col the index of the column, starting from 0
     * @return the value of the column in the current row
     */
    @BinaryOnly("getLocale")
    default Locale getLocaleColumn(int col)
    {
        return (Locale) toRow(get())[col];
    }

    /**
     * Hibernate 5 {@code getLong}.
     *
     * @param col the index of the column, starting from 0
     * @return the value of the column in the current row
     */
    @BinaryOnly("getLong")
    default Long getLongColumn(int col)
    {
        return (Long) toRow(get())[col];
    }

    /**
     * Hibernate 5 {@code getShort}.
     *
     * @param col the index of the column, starting from 0
     * @return the value of the column in the current row
     */
    @BinaryOnly("getShort")
    default Short getShortColumn(int col)
    {
        return (Short) toRow(get())[col];
    }

    /**
     * Hibernate 5 {@code getString}.
     *
     * @param col the index of the column, starting from 0
     * @return the value of the column in the current row
     */
    @BinaryOnly("getString")
    default String getStringColumn(int col)
    {
        return (String) toRow(get())[col];
    }

    /**
     * Hibernate 5 {@code getText}.
     *
     * @param col the index of the column, starting from 0
     * @return the value of the column in the current row
     */
    @BinaryOnly("getText")
    default String getTextColumn(int col)
    {
        return (String) toRow(get())[col];
    }

    /**
     * Hibernate 5 {@code getTimeZone}.
     *
     * @param col the index of the column, starting from 0
     * @return the value of the column in the current row
     */
    @BinaryOnly("getTimeZone")
    default TimeZone getTimeZoneColumn(int col)
    {
        return (TimeZone) toRow(get())[col];
    }
}
