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
package org.xwiki.livedata.script.livetable;

import java.util.Set;

import org.xwiki.stability.Unstable;

/**
 * What the authors picking a row naming strategy need to know about it.
 *
 * @param description the description of the strategy, in the current locale, or {@code null} when it has none
 * @param mandatoryProperties the live data ids of the properties the strategy needs a value for, when no source
 *     parameter is given
 * @version $Id$
 * @since 18.10.0RC1
 */
@Unstable
public record NewRowNamingStrategyDescriptor(String description, Set<String> mandatoryProperties)
{
}
