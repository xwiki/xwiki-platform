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
package org.xwiki.gwt.user.client;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Focusable;

/**
 * Command that sets focus on the specified widget.
 * <p>
 * We need the deferred command focus setting in order to handle the issue described at
 * http://code.google.com/p/google-web-toolkit/issues/detail?id=1849 and the applied workarounds in existing code (e.g.
 * GlassPanel).
 * 
 * @version $Id$
 */
public class FocusCommand implements Command
{
    /**
     * The widget to set focus to upon execution.
     */
    private Focusable widget;

    /**
     * Builds a focus command for the specified widget.
     * 
     * @param widget the widget to set focus to upon execution
     */
    public FocusCommand(Focusable widget)
    {
        this.widget = widget;
    }

    @Override
    public void execute()
    {
        widget.setFocus(true);
    }
}
