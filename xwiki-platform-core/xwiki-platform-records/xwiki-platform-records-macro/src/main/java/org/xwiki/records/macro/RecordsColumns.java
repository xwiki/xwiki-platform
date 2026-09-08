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
 * Marker type selecting this module's field picker for a macro parameter.
 * <p>
 * The parameter it is declared on stays a comma-separated {@link String}; this type exists only to name the HTML
 * displayer, since {@code DefaultTemplateHTMLDisplayer} resolves
 * {@code templates/html_displayer/<simple type name in lower case>/edit.vm} — here
 * {@code templates/html_displayer/recordscolumns/edit.vm}.
 * <p>
 * The widget it selects is a suggester whose candidate list depends on the value of the sibling {@code class}
 * parameter. The macro editor offers no declarative way to express that dependency: a displayer receives its own
 * type, default value and attributes and nothing else. What makes it work anyway is that the suggest widget's data
 * source is a callback invoked on each keystroke rather than a URL fixed when this template is rendered, so it can
 * read the {@code class} input at query time. See the {@code xwiki-platform-records-webjar} module for the
 * implementation, and note that the coupling is to the input <em>named after the parameter</em>, which is the same
 * contract the macro editor itself relies on to assemble a macro call.
 *
 * @version $Id$
 * @since 18.8.0RC1
 */
@Unstable
public interface RecordsColumns
{
}
