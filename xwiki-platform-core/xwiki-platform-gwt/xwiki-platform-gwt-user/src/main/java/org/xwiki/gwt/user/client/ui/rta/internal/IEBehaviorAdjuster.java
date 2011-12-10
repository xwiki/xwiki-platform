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
package org.xwiki.gwt.user.client.ui.rta.internal;

import org.xwiki.gwt.dom.client.Event;

import com.google.gwt.event.dom.client.KeyCodes;

/**
 * Adjusts the behavior of the rich text area in the newer versions of the Internet Explorer browser (9 and above).
 * 
 * @version $Id$
 */
public class IEBehaviorAdjuster extends BehaviorAdjuster
{
    @Override
    protected void onKeyDown(Event event)
    {
        if (event == null || event.isCancelled()) {
            return;
        }

        switch (event.getKeyCode()) {
            case KeyCodes.KEY_TAB:
                // IE moves the focus when Tab key is down and thus the key press event doesn't get fired. If we block
                // the key down event then IE doesn't fire key press. We are forced to apply out custom behavior for tab
                // key now, on key down, and not later, on key press.
                onTab(event);
                break;
            default:
                super.onKeyDown(event);
                break;
        }
    }
}
