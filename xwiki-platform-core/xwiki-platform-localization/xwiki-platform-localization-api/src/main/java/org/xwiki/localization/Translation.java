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
package org.xwiki.localization;

import java.util.Locale;

import org.xwiki.localization.message.TranslationMessage;
import org.xwiki.rendering.block.Block;

public interface Translation extends TranslationMessage
{
    /**
     * @return the bundle from which this translation comes
     */
    Bundle getBundle();

    /**
     * @return the locale of the translation
     */
    Locale getLocale();

    /**
     * @return the key of the translation
     */
    String getKey();

    /**
     * Execute the transformation (resolve any variable or parameter in its content) and produce a Block to insert in an
     * XDOM or to render as it is.
     * <p>
     * The variables are resolved using the current {@link BundleContext} bundles.
     * 
     * @param parameters the parameters
     * @return the result translation
     */
    Block render(Object... parameters);
}
