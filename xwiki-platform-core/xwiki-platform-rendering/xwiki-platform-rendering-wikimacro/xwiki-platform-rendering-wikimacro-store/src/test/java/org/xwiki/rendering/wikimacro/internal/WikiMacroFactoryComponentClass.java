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
package org.xwiki.rendering.wikimacro.internal;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.xwiki.component.wiki.internal.bridge.DefaultContentParser;
import org.xwiki.rendering.internal.macro.wikibridge.DefaultWikiMacroManager;
import org.xwiki.rendering.internal.macro.wikibridge.WikiMacroEventListener;
import org.xwiki.test.annotation.ComponentList;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Component list for the wiki macro factory. This list is currently used to load documents with wiki macro in page
 * tests.
 *
 * @version $Id$
 * @since 15.2RC1
 */
@Documented
@Retention(RUNTIME)
@Target({ TYPE, METHOD, ANNOTATION_TYPE })
@ComponentList({
    WikiMacroEventListener.class,
    DefaultWikiMacroFactory.class,
    DefaultWikiMacroManager.class,
    DefaultWikiMacro.class,
    DefaultContentParser.class,
    org.xwiki.rendering.internal.parser.DefaultContentParser.class,
    DefaultWikiMacroRenderer.class,
})
@Inherited
public @interface WikiMacroFactoryComponentClass
{
}
