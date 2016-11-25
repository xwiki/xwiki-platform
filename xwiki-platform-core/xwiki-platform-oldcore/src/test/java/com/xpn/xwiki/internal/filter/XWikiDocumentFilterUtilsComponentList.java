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
package com.xpn.xwiki.internal.filter;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.xwiki.filter.internal.DefaultFilterDescriptorManager;
import org.xwiki.filter.internal.converter.FilterEventParametersConverter;
import org.xwiki.filter.xar.internal.input.AttachmentReader;
import org.xwiki.filter.xar.internal.input.ClassPropertyReader;
import org.xwiki.filter.xar.internal.input.ClassReader;
import org.xwiki.filter.xar.internal.input.DocumentLocaleReader;
import org.xwiki.filter.xar.internal.input.WikiObjectReader;
import org.xwiki.filter.xar.internal.input.WikiReader;
import org.xwiki.filter.xar.internal.input.XARInputFilterStream;
import org.xwiki.filter.xar.internal.input.XARInputFilterStreamFactory;
import org.xwiki.filter.xar.internal.output.XAROutputFilterStream;
import org.xwiki.filter.xar.internal.output.XAROutputFilterStreamFactory;
import org.xwiki.properties.internal.DefaultBeanManager;
import org.xwiki.properties.internal.DefaultConverterManager;
import org.xwiki.properties.internal.converter.ConvertUtilsConverter;
import org.xwiki.properties.internal.converter.EnumConverter;
import org.xwiki.properties.internal.converter.LocaleConverter;
import org.xwiki.rendering.internal.transformation.DefaultRenderingContext;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.xar.internal.XarObjectPropertySerializerManager;
import org.xwiki.xar.internal.property.DateXarObjectPropertySerializer;
import org.xwiki.xar.internal.property.DefaultXarObjectPropertySerializer;
import org.xwiki.xar.internal.property.ListXarObjectPropertySerializer;

import com.xpn.xwiki.internal.filter.XWikiDocumentFilterUtils;
import com.xpn.xwiki.internal.filter.input.BaseClassEventGenerator;
import com.xpn.xwiki.internal.filter.input.BaseObjectEventGenerator;
import com.xpn.xwiki.internal.filter.input.BasePropertyEventGenerator;
import com.xpn.xwiki.internal.filter.input.PropertyClassEventGenerator;
import com.xpn.xwiki.internal.filter.input.XWikiAttachmentEventGenerator;
import com.xpn.xwiki.internal.filter.input.XWikiDocumentLocaleEventGenerator;
import com.xpn.xwiki.internal.filter.output.BaseClassOutputFilterStream;
import com.xpn.xwiki.internal.filter.output.BaseObjectOutputFilterStream;
import com.xpn.xwiki.internal.filter.output.BasePropertyOutputFilterStream;
import com.xpn.xwiki.internal.filter.output.PropertyClassOutputFilterStream;
import com.xpn.xwiki.internal.filter.output.XWikiAttachmentOutputFilterStream;
import com.xpn.xwiki.internal.filter.output.XWikiDocumentOutputFilterStream;
import com.xpn.xwiki.internal.localization.XWikiLocalizationContext;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Pack of default Component implementations that are needed to use XWikiDocumentFilterUtils.
 *
 * @version $Id$
 * @since 9.0RC1
 */
@Documented
@Retention(RUNTIME)
@Target({ TYPE, METHOD, ANNOTATION_TYPE })
@ComponentList({
    EnumConverter.class,
    ConvertUtilsConverter.class,
    DefaultConverterManager.class,
    DefaultBeanManager.class,
    DefaultFilterDescriptorManager.class,
    FilterEventParametersConverter.class,
    LocaleConverter.class,
    XWikiLocalizationContext.class,
    DefaultRenderingContext.class,

    // Document output
    BaseClassOutputFilterStream.class,
    BaseObjectOutputFilterStream.class,
    BasePropertyOutputFilterStream.class,
    PropertyClassOutputFilterStream.class,
    XWikiAttachmentOutputFilterStream.class,
    XWikiDocumentOutputFilterStream.class,

    // Document input
    BaseClassEventGenerator.class,
    BaseObjectEventGenerator.class,
    BasePropertyEventGenerator.class,
    PropertyClassEventGenerator.class,
    XWikiAttachmentEventGenerator.class,
    XWikiDocumentLocaleEventGenerator.class,

    // XAR
    XARInputFilterStreamFactory.class,
    XARInputFilterStream.class,
    XAROutputFilterStreamFactory.class,
    XAROutputFilterStream.class,
    XarObjectPropertySerializerManager.class,
    ListXarObjectPropertySerializer.class,
    DateXarObjectPropertySerializer.class,
    DefaultXarObjectPropertySerializer.class,
    DocumentLocaleReader.class,
    AttachmentReader.class,
    ClassPropertyReader.class,
    ClassReader.class,
    WikiObjectReader.class,
    WikiReader.class,

    // Entry point
    XWikiDocumentFilterUtils.class
})
@Inherited
public @interface XWikiDocumentFilterUtilsComponentList
{
}
