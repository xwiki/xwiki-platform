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
package org.xwiki.uiextension.script;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.xwiki.component.wiki.internal.DefaultWikiComponentManager;
import org.xwiki.component.wiki.internal.DefaultWikiComponentManagerContext;
import org.xwiki.component.wiki.internal.WikiComponentManagerEventListenerHelper;
import org.xwiki.component.wiki.internal.bridge.DefaultContentParser;
import org.xwiki.component.wiki.internal.bridge.DefaultWikiObjectComponentManagerEventListener;
import org.xwiki.component.wiki.internal.bridge.WikiObjectComponentManagerEventListenerProxy;
import org.xwiki.rendering.async.internal.block.DefaultBlockAsyncRenderer;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.uiextension.internal.DefaultUIExtensionManager;
import org.xwiki.uiextension.internal.WikiUIExtension;
import org.xwiki.uiextension.internal.WikiUIExtensionComponentBuilder;
import org.xwiki.uiextension.internal.filter.ExcludeFilter;
import org.xwiki.uiextension.internal.filter.SelectFilter;
import org.xwiki.uiextension.internal.filter.SortByCustomOrderFilter;
import org.xwiki.uiextension.internal.filter.SortByIdFilter;
import org.xwiki.uiextension.internal.filter.SortByParameterFilter;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Pack of default component implementations that are needed for {@link UIExtensionScriptService}.
 *
 * @version $Id$
 * @since 14.10.5
 * @since 15.1RC1
 */
@Documented
@Retention(RUNTIME)
@Target({TYPE, METHOD, ANNOTATION_TYPE})
@ComponentList({
    UIExtensionScriptService.class,
    DefaultUIExtensionManager.class,
    WikiUIExtensionComponentBuilder.class,
    WikiUIExtension.class,
    // Filters
    ExcludeFilter.class,
    SelectFilter.class,
    SortByCustomOrderFilter.class,
    SortByIdFilter.class,
    SortByParameterFilter.class,
    // Needed for registering the UI extensions defined in wiki pages.
    DefaultWikiObjectComponentManagerEventListener.class,
    WikiObjectComponentManagerEventListenerProxy.class,
    WikiComponentManagerEventListenerHelper.class,
    DefaultWikiComponentManager.class,
    DefaultWikiComponentManagerContext.class,
    // Needed for rendering the UI extensions.
    DefaultContentParser.class,
    DefaultBlockAsyncRenderer.class
})
@Inherited
public @interface UIExtensionScriptServiceComponentList
{
}
