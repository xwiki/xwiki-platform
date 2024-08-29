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
package org.xwiki.icon.internal;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.xwiki.bridge.internal.DefaultDocumentContextExecutor;
import org.xwiki.icon.internal.context.IconSetContext;
import org.xwiki.security.authorization.AuthorExecutor;
import org.xwiki.skinx.internal.CssDocumentSkinExtension;
import org.xwiki.skinx.internal.JsDocumentSkinExtension;
import org.xwiki.skinx.internal.JsFileSkinExtension;
import org.xwiki.skinx.internal.LinkSkinExtension;
import org.xwiki.test.annotation.ComponentList;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Pack of default Component implementations that are needed for running the {@link  DefaultIconManagerComponentList}.
 *
 * @version $Id$
 * @since 13.9RC1
 * @since 13.4.4
 */
@Documented
@Retention(RUNTIME)
@Target({ TYPE, METHOD, ANNOTATION_TYPE })
@ComponentList({
    DefaultIconManager.class,
    DefaultIconSetManager.class,
    DefaultIconSetCache.class,
    DefaultIconSetLoader.class,
    IconSetContext.class,
    DefaultIconRenderer.class,
    JsFileSkinExtension.class,
    CssDocumentSkinExtension.class,
    LinkSkinExtension.class,
    JsDocumentSkinExtension.class,
    VelocityRenderer.class,
    DefaultDocumentContextExecutor.class,
    AuthorExecutor.class,
})
@Inherited
public @interface DefaultIconManagerComponentList
{
}
