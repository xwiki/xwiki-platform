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
package org.xwiki.gwt.user.client.ui.rta.cmd;

/**
 * The code associated with a {@link Command}.
 * 
 * @version $Id$
 */
public interface Executable
{
    /**
     * Runs this executable with the given parameter.
     * 
     * @param param the execution parameter
     * @return {@code true} if the execution succeeds, {@code false} otherwise
     * @see CommandManager#execute(Command, String)
     */
    boolean execute(String param);

    /**
     * @return {@code true} if this executable is supported by its underlying execution target, {@code false} otherwise
     * @see CommandManager#isSupported(Command)
     */
    boolean isSupported();

    /**
     * @return {@code true} if this executable can be executed on its underlying execution target, {@code false}
     *         otherwise
     * @see CommandManager#isEnabled(Command)
     */
    boolean isEnabled();

    /**
     * @return {@code true} if this executable has been executed on its underlying execution target, {@code false}
     *         otherwise area.
     * @see CommandManager#isExecuted(Command)
     */
    boolean isExecuted();

    /**
     * @return the previous execution parameter, if {@link #isExecuted()} returns {@code true}, {@code null} otherwise
     */
    String getParameter();
}
