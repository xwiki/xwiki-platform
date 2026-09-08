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
 * Marker type selecting the XClass picker for a macro parameter.
 * <p>
 * The parameter it is declared on stays a {@link org.xwiki.model.reference.DocumentReference}; this type exists only
 * to name the HTML displayer, since {@code DefaultTemplateHTMLDisplayer} resolves
 * {@code templates/html_displayer/<simple type name in lower case>/edit.vm} — here
 * {@code templates/html_displayer/recordsdatatype/edit.vm}, which delegates to the {@code #classPicker} Velocity
 * macro.
 * <p>
 * That picker is the one the object and class editors use: it lists the XClasses the user may view, grouped by
 * space, by reference. It is a stopgap, not the picker this macro should end up with — searching on a human-readable
 * name, showing the entry count and hiding technical classes are the subject of a separate proposal.
 *
 * @version $Id$
 * @since 18.8.0RC1
 */
@Unstable
public interface RecordsDataType
{
}
