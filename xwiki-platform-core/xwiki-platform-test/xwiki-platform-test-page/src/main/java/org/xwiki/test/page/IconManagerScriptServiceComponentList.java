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
package org.xwiki.test.page;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.xwiki.icon.IconManagerScriptService;
import org.xwiki.icon.internal.DefaultIconManager;
import org.xwiki.icon.internal.DefaultIconRenderer;
import org.xwiki.icon.internal.DefaultIconSetCache;
import org.xwiki.icon.internal.DefaultIconSetLoader;
import org.xwiki.icon.internal.DefaultIconSetManager;
import org.xwiki.icon.internal.VelocityRenderer;
import org.xwiki.icon.internal.context.IconSetContext;
import org.xwiki.skinx.internal.CssDocumentSkinExtension;
import org.xwiki.skinx.internal.JsDocumentSkinExtension;
import org.xwiki.skinx.internal.LinkSkinExtension;
import org.xwiki.test.annotation.ComponentList;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Pack of {@link IconManagerScriptService} and its dependencies.
 *
 * @version $Id$
 * @since 13.4RC1
 */ 
@Documented
@Retention(RUNTIME)
@Target({ TYPE, METHOD, ANNOTATION_TYPE })
@ComponentList({
    IconManagerScriptService.class,
    DefaultIconManager.class,
    DefaultIconSetManager.class,
    DefaultIconSetCache.class,
    DefaultIconSetLoader.class,
    IconSetContext.class,
    DefaultIconRenderer.class,
    CssDocumentSkinExtension.class,
    LinkSkinExtension.class,
    JsDocumentSkinExtension.class,
    VelocityRenderer.class
})
@Inherited
public @interface IconManagerScriptServiceComponentList
{
}
