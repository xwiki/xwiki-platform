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
 * Marker type selecting this module's sort picker for a macro parameter.
 * <p>
 * The parameter it is declared on stays a comma-separated {@link String}; this type exists only to name the HTML
 * displayer, since {@code DefaultTemplateHTMLDisplayer} resolves
 * {@code templates/html_displayer/<simple type name in lower case>/edit.vm} — here
 * {@code templates/html_displayer/recordssort/edit.vm}.
 * <p>
 * The widget it selects offers each field of the data type twice, once ascending and once descending, so that a
 * criterion is picked rather than typed. It is a suggester rather than the platform's {@code sortPicker} widget
 * ({@code uicomponents/widgets/sortPicker.js}), which edits a single criterion through a field select and an order
 * select: this parameter holds an ordered <em>list</em> of criteria, and the suggester's drag-and-drop plugin is
 * what lets that order be authored. The platform widget would also need its field select filled from the sibling
 * {@code class} parameter, which is the same client-side work as this picker, so choosing it would buy the harder
 * half and still lose the list.
 * <p>
 * Like {@link RecordsColumns}, the candidate list depends on the value of the sibling {@code class} parameter, and
 * is read from the browser at query time for the reasons given there.
 *
 * @version $Id$
 * @since 18.8.0RC1
 */
@Unstable
public interface RecordsSort
{
}
