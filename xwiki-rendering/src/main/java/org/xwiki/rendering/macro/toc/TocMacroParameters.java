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
package org.xwiki.rendering.macro.toc;

import org.xwiki.rendering.macro.parameter.ParameterValueTooLowException;

/**
 * Parameters for the {@link org.xwiki.rendering.internal.macro.toc.TocMacro} Macro.
 *
 * @version $Id: $
 * @since 1.6M1
 */
public class TocMacroParameters
{
    /**
     * @version $Id: TocMacroDescriptor.java 11982 2008-08-22 17:49:14Z tmortagne $
     */
    public enum Scope
    {
        /**
         * List section starting where macro block is located in the XDOM.
         */
        LOCAL,

        /**
         * List the sections of the whole page.
         */
        PAGE;
    };

    private int start;

    private int depth;

    private Scope scope;

    private boolean numbered;

    /**
     * @return the minimum section level. For example if 2 then level 1 sections will not be listed.
     */
    public int getStart()
    {
        return this.start;
    }

    public void setStart(int start) throws ParameterValueTooLowException
    {
        if (start < 1) {
            throw new ParameterValueTooLowException(1);
        }

        this.start = start;
    }

    /**
     * @return the maximum section level. For example if 3 then all section levels from 4 will not be listed.
     */
    public int getDepth()
    {
        return this.depth;
    }

    public void setDepth(int depth) throws ParameterValueTooLowException
    {
        if (depth < 1) {
            throw new ParameterValueTooLowException(1);
        }

        this.depth = depth;
    }

    /**
     * @return local or page. If local only section in the current scope will be listed. For example if the macro is
     *         written in a section, only subsections of this section will be listed.
     */
    public Scope getScope()
    {
        return this.scope;
    }

    public void setScope(Scope scope)
    {
        this.scope = scope;
    }

    /**
     * @return true or false. If true the section title number is printed.
     */
    public boolean numbered()
    {
        return this.numbered;
    }

    public void setNumbered(boolean numbered)
    {
        this.numbered = numbered;
    }
}
