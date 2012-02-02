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
package org.xwiki.tools.jetty.listener;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Localization helper.
 * 
 * @version $Id$
 * @since 3.5M1
 */
public final class Messages
{
    /** The name of the resource bundle holding the translations. */
    private static final String BUNDLE_NAME = "org.xwiki.tools.jetty.listener.messages";

    /** The resource bundle. */
    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

    /**
     * Default constructor, private since this is an utility class that should not be instantiated.
     */
    private Messages()
    {
        // Nothing to do.
    }

    /**
     * Get translation corresponding to the given key.
     * 
     * @param key the localization key to translate
     * @return the translation for the given key, in the current system locale
     */
    public static String getString(String key)
    {
        try {
            return RESOURCE_BUNDLE.getString(key);
        } catch (MissingResourceException e) {
            return '!' + key + '!';
        }
    }
}
