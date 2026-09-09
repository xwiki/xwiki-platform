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
package org.xwiki.records.macro;

import org.xwiki.stability.Unstable;

/**
 * Marker type selecting the layout picker for a macro parameter.
 * <p>
 * The parameter it is declared on stays a {@link String} holding a Live Data layout identifier; this type exists
 * only to name the HTML displayer, since {@code DefaultTemplateHTMLDisplayer} resolves
 * {@code templates/html_displayer/<simple type name in lower case>/edit.vm} — here
 * {@code templates/html_displayer/recordslayouts/edit.vm}.
 * <p>
 * The widget it selects is a pair of radio buttons naming the two layouts this macro offers, {@code table} and
 * {@code cards}. The choice is deliberately closed: Live Data accepts a comma-separated list of any registered
 * layout, but the layouts an author can reasonably pick for a table of records are these two, and a free-text list
 * asks them to know identifiers the dialog is in a position to name. An author who needs anything else — a third
 * layout, or a switcher between several — reaches for the {@code liveData} macro, which is the unconstrained
 * surface.
 * <p>
 * Radio buttons work here without any JavaScript of their own because the macro editor already handles them: it
 * collects the inputs whose {@code name} matches the parameter id, and when the first of them is a radio it keeps
 * the whole group and checks the one whose value matches the current parameter value.
 *
 * @version $Id$
 * @since 18.8.0RC1
 */
@Unstable
public interface RecordsLayouts
{
}
