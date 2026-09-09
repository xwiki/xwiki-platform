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
 * Marker type selecting this module's filter picker for a macro parameter.
 * <p>
 * The parameter it is declared on stays a query string; this type exists only to name the HTML displayer, since
 * {@code DefaultTemplateHTMLDisplayer} resolves
 * {@code templates/html_displayer/<simple type name in lower case>/edit.vm} — here
 * {@code templates/html_displayer/recordsfilters/edit.vm}.
 * <p>
 * The widget it selects holds one item per {@code field=value} constraint, which is what the renderer parses the
 * query string into. Its suggestions come in two steps, since only the author knows which field they mean: the
 * fields until a {@code =} is typed, then that field's values. Those values are not invented here — the Live Data
 * properties resource reports a {@code filter.searchURL} for every property whose values are enumerable, and that
 * is the same URL the Live Data filter row queries, so the dialog offers what a reader would be offered in the
 * rendered table. A property without one is filtered by typing, which is why this is the only picker of this
 * dialog that accepts free text.
 * <p>
 * Like {@link RecordsColumns}, the candidate list depends on the value of the sibling {@code class} parameter, and
 * is read from the browser at query time for the reasons given there.
 *
 * @version $Id$
 * @since 18.8.0RC1
 */
@Unstable
public interface RecordsFilters
{
}
