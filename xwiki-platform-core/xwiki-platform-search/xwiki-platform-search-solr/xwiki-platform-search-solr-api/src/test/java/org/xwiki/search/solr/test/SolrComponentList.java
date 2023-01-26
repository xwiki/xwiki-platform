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
package org.xwiki.search.solr.test;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.xwiki.component.internal.ContextComponentManagerProvider;
import org.xwiki.properties.internal.DefaultConverterManager;
import org.xwiki.properties.internal.converter.ConvertUtilsConverter;
import org.xwiki.properties.internal.converter.EnumConverter;
import org.xwiki.search.solr.internal.DefaultSolr;
import org.xwiki.search.solr.internal.DefaultSolrConfiguration;
import org.xwiki.search.solr.internal.DefaultSolrUtils;
import org.xwiki.search.solr.internal.EmbeddedSolr;
import org.xwiki.search.solr.internal.SolrSchemaUtils;
import org.xwiki.test.XWikiPropertiesMemoryConfigurationSource;
import org.xwiki.test.annotation.ComponentList;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Pack of default Component implementations that are needed for running Solr.
 *
 * @version $Id$
 * @since 12.3RC1
 */
@Documented
@Retention(RUNTIME)
@Target({ TYPE, METHOD, ANNOTATION_TYPE })
@ComponentList({
    DefaultSolr.class,
    EmbeddedSolr.class,
    DefaultSolrUtils.class,
    DefaultConverterManager.class,
    ConvertUtilsConverter.class,
    DefaultSolrConfiguration.class,
    EnumConverter.class,
    ContextComponentManagerProvider.class,
    XWikiPropertiesMemoryConfigurationSource.class,
    SolrSchemaUtils.class
})
@Inherited
public @interface SolrComponentList
{
}
