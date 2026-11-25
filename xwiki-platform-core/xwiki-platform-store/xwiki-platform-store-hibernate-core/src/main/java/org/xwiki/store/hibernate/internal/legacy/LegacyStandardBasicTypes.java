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
package org.xwiki.store.hibernate.internal.legacy;

import java.io.Serializable;

import org.hibernate.type.BigDecimalType;
import org.hibernate.type.BigIntegerType;
import org.hibernate.type.BinaryType;
import org.hibernate.type.BlobType;
import org.hibernate.type.BooleanType;
import org.hibernate.type.ByteType;
import org.hibernate.type.CalendarDateType;
import org.hibernate.type.CalendarType;
import org.hibernate.type.CharArrayType;
import org.hibernate.type.CharacterArrayType;
import org.hibernate.type.CharacterType;
import org.hibernate.type.ClassType;
import org.hibernate.type.ClobType;
import org.hibernate.type.CurrencyType;
import org.hibernate.type.DateType;
import org.hibernate.type.DoubleType;
import org.hibernate.type.FloatType;
import org.hibernate.type.ImageType;
import org.hibernate.type.IntegerType;
import org.hibernate.type.LocaleType;
import org.hibernate.type.LongType;
import org.hibernate.type.MaterializedBlobType;
import org.hibernate.type.MaterializedClobType;
import org.hibernate.type.MaterializedNClobType;
import org.hibernate.type.NClobType;
import org.hibernate.type.NTextType;
import org.hibernate.type.NumericBooleanType;
import org.hibernate.type.RowVersionType;
import org.hibernate.type.SerializableType;
import org.hibernate.type.ShortType;
import org.hibernate.type.StringNVarcharType;
import org.hibernate.type.StringType;
import org.hibernate.type.TextType;
import org.hibernate.type.TimeType;
import org.hibernate.type.TimeZoneType;
import org.hibernate.type.TimestampType;
import org.hibernate.type.TrueFalseType;
import org.hibernate.type.UUIDBinaryType;
import org.hibernate.type.UUIDCharType;
import org.hibernate.type.UrlType;
import org.hibernate.type.WrapperBinaryType;
import org.hibernate.type.YesNoType;
import org.hibernate.type.StandardBasicTypes;

/**
 * The constants of {@link StandardBasicTypes} with the type they had in Hibernate 5.
 * <p>
 * This interface is added to the super interfaces of {@link StandardBasicTypes} in the patched Hibernate: the Java
 * Virtual Machine resolves a field from its name and its type, and looks for it in the super interfaces when the class
 * itself does not declare it, so the code compiled against Hibernate 5 (expecting for example a {@code STRING} field
 * of type {@code StringType}) gets these constants while the Java compiler keeps seeing the fields of
 * {@link StandardBasicTypes} (which hide the ones of this interface).
 *
 * @version $Id$
 * @since 19.0.0RC1
 */
@SuppressWarnings({ "deprecation", "checkstyle:InterfaceIsType" })
public interface LegacyStandardBasicTypes
{
    /**
     * @see StandardBasicTypes#BOOLEAN
     */
    BooleanType BOOLEAN = BooleanType.INSTANCE;

    /**
     * @see StandardBasicTypes#NUMERIC_BOOLEAN
     */
    NumericBooleanType NUMERIC_BOOLEAN = NumericBooleanType.INSTANCE;

    /**
     * @see StandardBasicTypes#TRUE_FALSE
     */
    TrueFalseType TRUE_FALSE = TrueFalseType.INSTANCE;

    /**
     * @see StandardBasicTypes#YES_NO
     */
    YesNoType YES_NO = YesNoType.INSTANCE;

    /**
     * @see StandardBasicTypes#BYTE
     */
    ByteType BYTE = ByteType.INSTANCE;

    /**
     * @see StandardBasicTypes#SHORT
     */
    ShortType SHORT = ShortType.INSTANCE;

    /**
     * @see StandardBasicTypes#INTEGER
     */
    IntegerType INTEGER = IntegerType.INSTANCE;

    /**
     * @see StandardBasicTypes#LONG
     */
    LongType LONG = LongType.INSTANCE;

    /**
     * @see StandardBasicTypes#FLOAT
     */
    FloatType FLOAT = FloatType.INSTANCE;

    /**
     * @see StandardBasicTypes#DOUBLE
     */
    DoubleType DOUBLE = DoubleType.INSTANCE;

    /**
     * @see StandardBasicTypes#BIG_INTEGER
     */
    BigIntegerType BIG_INTEGER = BigIntegerType.INSTANCE;

    /**
     * @see StandardBasicTypes#BIG_DECIMAL
     */
    BigDecimalType BIG_DECIMAL = BigDecimalType.INSTANCE;

    /**
     * @see StandardBasicTypes#CHARACTER
     */
    CharacterType CHARACTER = CharacterType.INSTANCE;

    /**
     * @see StandardBasicTypes#STRING
     */
    StringType STRING = StringType.INSTANCE;

    /**
     * @see StandardBasicTypes#NSTRING
     */
    StringNVarcharType NSTRING = StringNVarcharType.INSTANCE;

    /**
     * @see StandardBasicTypes#URL
     */
    UrlType URL = UrlType.INSTANCE;

    /**
     * @see StandardBasicTypes#TIME
     */
    TimeType TIME = TimeType.INSTANCE;

    /**
     * @see StandardBasicTypes#DATE
     */
    DateType DATE = DateType.INSTANCE;

    /**
     * @see StandardBasicTypes#TIMESTAMP
     */
    TimestampType TIMESTAMP = TimestampType.INSTANCE;

    /**
     * @see StandardBasicTypes#CALENDAR
     */
    CalendarType CALENDAR = CalendarType.INSTANCE;

    /**
     * @see StandardBasicTypes#CALENDAR_DATE
     */
    CalendarDateType CALENDAR_DATE = CalendarDateType.INSTANCE;

    /**
     * @see StandardBasicTypes#CLASS
     */
    ClassType CLASS = ClassType.INSTANCE;

    /**
     * @see StandardBasicTypes#LOCALE
     */
    LocaleType LOCALE = LocaleType.INSTANCE;

    /**
     * @see StandardBasicTypes#CURRENCY
     */
    CurrencyType CURRENCY = CurrencyType.INSTANCE;

    /**
     * @see StandardBasicTypes#TIMEZONE
     */
    TimeZoneType TIMEZONE = TimeZoneType.INSTANCE;

    /**
     * @see StandardBasicTypes#UUID_BINARY
     */
    UUIDBinaryType UUID_BINARY = UUIDBinaryType.INSTANCE;

    /**
     * @see StandardBasicTypes#UUID_CHAR
     */
    UUIDCharType UUID_CHAR = UUIDCharType.INSTANCE;

    /**
     * @see StandardBasicTypes#BINARY
     */
    BinaryType BINARY = BinaryType.INSTANCE;

    /**
     * @see StandardBasicTypes#BINARY_WRAPPER
     */
    WrapperBinaryType WRAPPER_BINARY = WrapperBinaryType.INSTANCE;

    /**
     * @see StandardBasicTypes#ROW_VERSION
     */
    RowVersionType ROW_VERSION = RowVersionType.INSTANCE;

    /**
     * @see StandardBasicTypes#IMAGE
     */
    ImageType IMAGE = ImageType.INSTANCE;

    /**
     * @see StandardBasicTypes#BLOB
     */
    BlobType BLOB = BlobType.INSTANCE;

    /**
     * @see StandardBasicTypes#MATERIALIZED_BLOB
     */
    MaterializedBlobType MATERIALIZED_BLOB = MaterializedBlobType.INSTANCE;

    /**
     * @see StandardBasicTypes#CHAR_ARRAY
     */
    CharArrayType CHAR_ARRAY = CharArrayType.INSTANCE;

    /**
     * @see StandardBasicTypes#CHARACTER_ARRAY
     */
    CharacterArrayType CHARACTER_ARRAY = CharacterArrayType.INSTANCE;

    /**
     * @see StandardBasicTypes#TEXT
     */
    TextType TEXT = TextType.INSTANCE;

    /**
     * @see StandardBasicTypes#NTEXT
     */
    NTextType NTEXT = NTextType.INSTANCE;

    /**
     * @see StandardBasicTypes#CLOB
     */
    ClobType CLOB = ClobType.INSTANCE;

    /**
     * @see StandardBasicTypes#NCLOB
     */
    NClobType NCLOB = NClobType.INSTANCE;

    /**
     * @see StandardBasicTypes#MATERIALIZED_CLOB
     */
    MaterializedClobType MATERIALIZED_CLOB = MaterializedClobType.INSTANCE;

    /**
     * @see StandardBasicTypes#MATERIALIZED_NCLOB
     */
    MaterializedNClobType MATERIALIZED_NCLOB = MaterializedNClobType.INSTANCE;

    /**
     * @see StandardBasicTypes#SERIALIZABLE
     */
    SerializableType<Serializable> SERIALIZABLE = SerializableType.INSTANCE;
}
