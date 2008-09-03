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
package com.xpn.xwiki.wysiwyg.client.ui.cmd;

import com.google.gwt.user.client.Element;

/**
 * The code associated with a {@link Command}.
 */
public interface Executable
{
    /**
     * Executes the associated {@link Command} on the specified target with the given parameter.
     * 
     * @param target Execution target.
     * @param param Execution parameter.
     * @return true if execution succeeds.
     * @see CommandManager#execute(Command, String)
     */
    boolean execute(Element target, String param);

    /**
     * @param target Execution target.
     * @return true if the associated {@link Command} is supported by the specified target.
     * @see CommandManager#isSupported(Command)
     */
    boolean isSupported(Element target);

    /**
     * @param target Execution target.
     * @return true if the associated {@link Command} can be executed on the current state of the given target.
     * @see CommandManager#isEnabled(Command)
     */
    boolean isEnabled(Element target);

    /**
     * @param target Execution target.
     * @return true if the associated {@link Command} has been executed on the current state of the given target.
     * @see CommandManager#isExecuted(Command)
     */
    boolean isExecuted(Element target);

    /**
     * @param target Execution target.
     * @return the previous execution parameter, if {@link #isExecuted(Element)} returns true, null otherwise.
     */
    String getParameter(Element target);
}
