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
package org.xwiki.localization.message;

import java.util.Collection;
import java.util.Locale;

import org.xwiki.localization.TranslationBundle;
import org.xwiki.rendering.block.Block;

/**
 * An element in a translation message.
 * 
 * @version $Id$
 * @since 5.0M2
 */
public interface TranslationMessageElement
{
    /**
     * Execute the transformation (resolve any variable or parameter in its content) and produce a Block to insert in an
     * into a XDOM or to render as it is.
     * 
     * @param locale the locale to used to resolve variables
     * @param bundles the bundles to resolve variables with
     * @param parameters the parameters
     * @return the result translation
     */
    Block render(Locale locale, Collection<TranslationBundle> bundles, Object... parameters);
}
