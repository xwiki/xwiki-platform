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
package org.xwiki.rendering.macro.code;

import org.xwiki.rendering.internal.code.layout.CodeLayoutHandler;
import org.xwiki.stability.Unstable;

/**
 * Values allowed in the <code>layout</code> parameter of the code macro.
 * 
 * @version $Id$
 * @since 11.5RC1
 */
@Unstable
public enum CodeMacroLayout
{
    /**
     * Output the blocks as rendered by the parser.
     */
    PLAIN(Constants.PLAIN_HINT),
    /**
     * Display line numbers beside the rendered code.
     */
    LINENUMBERS(Constants.LINENUMBERS_HINT);

    private String hint;

    CodeMacroLayout(String hint)
    {
        this.hint = hint;
    }

    /**
     * @return the hint value to request the proper layout handler via DI.
     */
    public final String getHint()
    {
        return hint;
    }

    /**
     * Convenience class holding hint values for {@link CodeLayoutHandler} components. Links the outer enum class to the
     * components classes as it is not possible to reference enums in annotations (and avoids using plain strings).
     * 
     * @version $Id$
     */
    public static class Constants
    {
        /**
         * @see CodeMacroLayout#PLAIN
         */
        public static final String PLAIN_HINT = "plain";

        /**
         * @see CodeMacroLayout#LINENUMBERS
         */
        public static final String LINENUMBERS_HINT = "linenumbers";
    }
}
