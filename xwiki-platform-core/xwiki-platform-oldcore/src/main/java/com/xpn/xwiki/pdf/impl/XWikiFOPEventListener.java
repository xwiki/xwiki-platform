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
package com.xpn.xwiki.pdf.impl;

import org.apache.fop.events.Event;
import org.apache.fop.events.EventListener;
import org.apache.fop.events.model.EventSeverity;

/**
 * Prevent errors happening in FOP from stopping the PDF export. For example unrecognized FOP properties are ignored
 * and don't generate an exception.
 *
 * @version $Id$
 * @since 9.4RC1
 */
public class XWikiFOPEventListener implements EventListener
{
    /**
     * @param event the FOP event received, see
     *        <a href="https://xmlgraphics.apache.org/fop/trunk/events.html#consumer">FOP events</a>
     */
    public void processEvent(Event event)
    {
        // Transform fatal events into warnings in order to not stop the PDF export process
        if (event.getSeverity().equals(EventSeverity.FATAL)) {
            event.setSeverity(EventSeverity.WARN);
        }
    }
}
