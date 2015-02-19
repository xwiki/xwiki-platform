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
package com.xpn.xwiki.objects.classes;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.localization.LocalizationContext;
import org.xwiki.xar.internal.property.DateXarObjectPropertySerializer;

import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.DateProperty;
import com.xpn.xwiki.objects.meta.PropertyMetaClass;
import com.xpn.xwiki.web.Utils;

/**
 * Defines an XClass property type whose value is a Date.
 *
 * @version $Id$
 */
public class DateClass extends PropertyClass
{
    /** Logging helper object. */
    private static final Logger LOGGER = LoggerFactory.getLogger(DateClass.class);

    /**
     * The meta property that specifies if a date picker should be used in edit mode to choose the date.
     */
    private static final String META_PROPERTY_PICKER = "picker";

    /**
     * The meta property that controls the size of the date input in edit mode.
     */
    private static final String META_PROPERTY_SIZE = "size";

    /**
     * The meta property that controls is an empty date value represents the current date.
     */
    private static final String META_PROPERTY_EMPTY_IS_TODAY = "emptyIsToday";

    /**
     * The meta property that specifies the date format.
     */
    private static final String META_PROPERTY_DATE_FORMAT = "dateFormat";

    /**
     * Used to get the current locale.
     */
    private LocalizationContext localizationContext;

    /**
     * Creates a new Date property that is described by the given meta class.
     *
     * @param metaClass the meta class that defines the list of meta properties associated with this property type
     */
    public DateClass(PropertyMetaClass metaClass)
    {
        super("date", "Date", metaClass);

        setSize(20);
        setDateFormat("dd/MM/yyyy HH:mm:ss");
        setEmptyIsToday(1);
        setPicker(1);
    }

    /**
     * Default constructor.
     */
    public DateClass()
    {
        this(null);
    }

    /**
     * @return {@code 1} if a date picker should be used to choose the date, {@code 0} otherwise
     */
    public int getPicker()
    {
        return getIntValue(META_PROPERTY_PICKER);
    }

    /**
     * Sets whether to use a date picker or not to select the date in edit mode.
     *
     * @param picker {@code 1} to use a date picker, {@code 0} otherwise
     */
    public void setPicker(int picker)
    {
        setIntValue(META_PROPERTY_PICKER, picker);
    }

    /**
     * @return the size of the date input in edit mode
     */
    public int getSize()
    {
        return getIntValue(META_PROPERTY_SIZE);
    }

    /**
     * Sets the size of the date input in edit mode.
     *
     * @param size the size of the date input in edit mode
     */
    public void setSize(int size)
    {
        setIntValue(META_PROPERTY_SIZE, size);
    }

    /**
     * @return {@code 1} if an empty date value represents the current date, {@code 0} otherwise
     */
    public int getEmptyIsToday()
    {
        return getIntValue(META_PROPERTY_EMPTY_IS_TODAY);
    }

    /**
     * Sets whether an empty date value represents the current date or not.
     *
     * @param emptyIsToday {@code 1} if an empty date value should represent the current date, {@code 0} otherwise
     */
    public void setEmptyIsToday(int emptyIsToday)
    {
        setIntValue(META_PROPERTY_EMPTY_IS_TODAY, emptyIsToday);
    }

    /**
     * @return the date format
     */
    public String getDateFormat()
    {
        return getStringValue(META_PROPERTY_DATE_FORMAT);
    }

    /**
     * Sets the date format.
     *
     * @param format the new date format
     */
    public void setDateFormat(String format)
    {
        setStringValue(META_PROPERTY_DATE_FORMAT, format);
    }

    @Override
    public BaseProperty fromString(String value)
    {
        BaseProperty property = newProperty();

        if (StringUtils.isEmpty(value)) {
            if (getEmptyIsToday() == 1) {
                property.setValue(new Date());
            }
            return property;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat(getDateFormat(), getCurrentLocale());
            property.setValue(sdf.parse(value));
            return property;
        } catch (ParseException e) {
            return null;
        }
    }

    @Override
    public BaseProperty newProperty()
    {
        BaseProperty property = new DateProperty();
        property.setName(getName());
        return property;
    }

    /**
     * @param property a date property
     * @return the value of the given date property serialized using the {@link #getDateFormat()} format
     */
    public String toFormString(BaseProperty property)
    {
        return property.getValue() == null ? "" : new SimpleDateFormat(getDateFormat(), getCurrentLocale())
            .format(property.getValue());
    }

    /**
     * {@inheritDoc}
     * <p>
     * We have to overwrite this method because the value of a date property is not serialized using the date format
     * specified in the XClass nor the time stamp but a custom hard-coded date format.. Changing this now will break
     * existing XARs..
     */
    @Override
    public BaseProperty newPropertyfromXML(Element element)
    {
        String value = element.getText();
        BaseProperty property = newProperty();

        if (StringUtils.isEmpty(value)) {
            return property;
        }

        // FIXME: The value of a date property should be serialized using the date timestamp or the date format
        // specified in the XClass the date property belongs to.
        SimpleDateFormat sdf = DateXarObjectPropertySerializer.DEFAULT_FORMAT;
        try {
            property.setValue(sdf.parse(value));
        } catch (ParseException e) {
            // I suppose this is a date format used a long time ago. DateProperty is using the above date format now.
            SimpleDateFormat sdfOld = new SimpleDateFormat("EEE MMM d HH:mm:ss z yyyy", Locale.US);
            LOGGER.warn("Failed to parse date [{}] using format [{}]. Trying again with format [{}].", value,
                sdf.toString(), sdfOld.toString());
            try {
                property.setValue(sdfOld.parse(value));
            } catch (ParseException exception) {
                LOGGER.warn("Failed to parse date [{}] using format [{}]. Defaulting to the current date.", value,
                    sdfOld.toString());
                property.setValue(new Date());
            }
        }
        return property;
    }

    private LocalizationContext getLocalizationContext()
    {
        if (this.localizationContext == null) {
            this.localizationContext = Utils.getComponent(LocalizationContext.class);
        }
        return this.localizationContext;
    }

    private Locale getCurrentLocale()
    {
        try {
            return getLocalizationContext().getCurrentLocale();
        } catch (Exception e) {
            Locale defaultLocale = Locale.getDefault();
            LOGGER.warn("Failed to get the context locale: [{}]. Continue using the default JVM locale [{}].",
                ExceptionUtils.getRootCauseMessage(e), defaultLocale);
            return defaultLocale;
        }
    }
}
