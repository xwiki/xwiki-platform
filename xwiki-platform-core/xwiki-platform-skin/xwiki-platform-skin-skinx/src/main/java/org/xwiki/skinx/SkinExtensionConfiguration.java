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
package org.xwiki.skinx;

import org.xwiki.component.annotation.Role;

/**
 * Configuration options for skin extensions.
 * 
 * @version $Id$
 * @since 12.7.1
 * @since 12.8RC1
 */
@Role
public interface SkinExtensionConfiguration
{
    /**
     * See https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Strict_mode .
     * <p>
     * When strict mode is enabled:
     * <ul>
     * <li>the JavaScript minification may fail if the code is poorly written. See
     * https://github.com/google/closure-compiler/wiki/Warnings for a list of errors that may occur. When this happens
     * XWiki returns the original (unminified) JavaScript source.</li>
     * <li>the minified JavaScript includes the "use strict;" statement which means the code may fail at runtime if it
     * doesn't follow the ECMAScript strict rules.</li>
     * </ul>
     * 
     * @return {@code true} if the JavaScript skin extensions should be parsed in strict mode when minified at runtime
     *         and whether the minified JavaScript should include the "use strict;" statement that enables the execution
     *         of JavaScript in strict mode for browsers that supports it, {@code false} otherwise
     */
    boolean shouldRunJavaScriptInStrictMode();
}
