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
package org.xwiki.wikistream.internal.filter;

import org.xwiki.rendering.listener.Listener;
import org.xwiki.wikistream.filter.AttachmentFilter;
import org.xwiki.wikistream.filter.ClassFilter;
import org.xwiki.wikistream.filter.ClassPropertyFilter;
import org.xwiki.wikistream.filter.DocumentFilter;
import org.xwiki.wikistream.filter.FarmFilter;
import org.xwiki.wikistream.filter.ObjectFilter;
import org.xwiki.wikistream.filter.ObjectPropertyFilter;
import org.xwiki.wikistream.filter.SpaceFilter;
import org.xwiki.wikistream.filter.UnknownFilter;
import org.xwiki.wikistream.filter.WikiFilter;

public interface AllFilter extends FarmFilter, WikiFilter, SpaceFilter, DocumentFilter, AttachmentFilter, ClassFilter,
    ClassPropertyFilter, ObjectFilter, ObjectPropertyFilter, UnknownFilter, Listener
{

}
