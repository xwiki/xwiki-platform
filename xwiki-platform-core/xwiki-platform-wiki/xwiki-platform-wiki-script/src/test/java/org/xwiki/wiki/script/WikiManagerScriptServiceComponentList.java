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
package org.xwiki.wiki.script;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.xwiki.test.annotation.ComponentList;
import org.xwiki.url.internal.standard.DefaultStandardURLConfiguration;
import org.xwiki.wiki.internal.configuration.DefaultWikiConfiguration;
import org.xwiki.wiki.internal.descriptor.builder.DefaultWikiDescriptorBuilder;
import org.xwiki.wiki.internal.descriptor.document.DefaultWikiDescriptorDocumentHelper;
import org.xwiki.wiki.internal.manager.DefaultWikiCreator;
import org.xwiki.wiki.internal.manager.DefaultWikiDeleter;
import org.xwiki.wiki.internal.manager.DefaultWikiManager;
import org.xwiki.wiki.internal.provisioning.DefaultWikiCopier;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Pack of default Components that are needed for the wiki manager script service.
 *
 * @version $Id$
 * @since 14.10
 */
@Documented
@Retention(RUNTIME)
@Target({ TYPE, METHOD, ANNOTATION_TYPE })
@ComponentList({
    WikiManagerScriptService.class,
    DefaultWikiManager.class,
    DefaultWikiCreator.class,
    DefaultWikiDescriptorBuilder.class,
    DefaultWikiDescriptorDocumentHelper.class,
    DefaultWikiConfiguration.class,
    DefaultWikiCopier.class,
    DefaultWikiDeleter.class,
    DefaultStandardURLConfiguration.class
})
@Inherited
public @interface WikiManagerScriptServiceComponentList
{
}
