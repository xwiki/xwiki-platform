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
package org.xwiki.gwt.wysiwyg.client.gadget;

import org.xwiki.gwt.wysiwyg.client.plugin.macro.MacroCall;

/**
 * Stores the information about a gadget instance, the macro call which is the content of the macro, and the title, to
 * be returned by {@link EditGadgetWizardStep} as a result.
 * 
 * @version $Id$
 * @since 3.0M3
 */
public class GadgetInstance
{
    /**
     * The macro call that represents the content of this gadget.
     */
    private MacroCall macroCall;

    /**
     * The title of this gadget.
     */
    private String title;

    /**
     * @return the macroCall
     */
    public MacroCall getMacroCall()
    {
        return macroCall;
    }

    /**
     * @param macroCall the macroCall to set
     */
    public void setMacroCall(MacroCall macroCall)
    {
        this.macroCall = macroCall;
    }

    /**
     * @return the title
     */
    public String getTitle()
    {
        return title;
    }

    /**
     * @param title the title to set
     */
    public void setTitle(String title)
    {
        this.title = title;
    }
}
