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
package org.xwiki.xar.internal.property;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.xar.internal.XarObjectPropertySerializer;

/**
 * {@link Date} based implementation of {@link XarObjectPropertySerializer}.
 * 
 * @version $Id$
 * @since 5.4M1
 */
@Component
@Named("Date")
@Singleton
public class DateXarObjectPropertySerializer implements XarObjectPropertySerializer
{
    /**
     * The default {@link String} format used to serialize and parse the date.
     */
    public static final SimpleDateFormat DEFAULT_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");

    @Inject
    private Logger logger;

    @Override
    public Object read(XMLStreamReader reader) throws XMLStreamException
    {
        String value = reader.getElementText();

        if (StringUtils.isEmpty(value)) {
            return null;
        }

        // FIXME: The value of a date property should be serialized using the date timestamp or the date format
        // specified in the XClass the date property belongs to.
        SimpleDateFormat sdf = DEFAULT_FORMAT;
        try {
            return sdf.parse(value);
        } catch (ParseException e) {
            // I suppose this is a date format used a long time ago. DateProperty is using the above date format now.
            SimpleDateFormat sdfOld = new SimpleDateFormat("EEE MMM d HH:mm:ss z yyyy", Locale.US);
            this.logger.warn("Failed to parse date [{}] using format [{}]. Trying again with format [{}].", value,
                sdf.toPattern(), sdfOld.toPattern());
            try {
                return sdfOld.parse(value);
            } catch (ParseException exception) {
                this.logger.warn("Failed to parse date [{}] using format [{}]. Defaulting to the current date.", value,
                    sdfOld.toPattern());
                return new Date();
            }
        }
    }

    @Override
    public void write(XMLStreamWriter writer, Object value) throws XMLStreamException
    {
        // FIXME: The value of a date property should be serialized using the date timestamp or the date format
        // specified in the XClass the date property belongs to.
        writer.writeCharacters(value == null ? "" : DEFAULT_FORMAT.format(value));
    }
}
