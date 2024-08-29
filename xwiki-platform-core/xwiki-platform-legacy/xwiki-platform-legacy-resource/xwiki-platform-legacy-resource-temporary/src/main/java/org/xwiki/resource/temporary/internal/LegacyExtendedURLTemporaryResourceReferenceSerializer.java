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
package org.xwiki.resource.temporary.internal;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

/**
 * An error was made in the component hint when introducing {@link ExtendedURLTemporaryResourceReferenceSerializer}
 * originally ({@code standard/tmp} was used when it should have been a default hint). We're providing this class to
 * provide backward compatibility for contrib extensions and anyone using the old hint.
 *
 * @version $Id$
 * @since 14.7
 */
@Component
@Named("standard/tmp")
@Singleton
@Deprecated(since = "14.7RC1")
public class LegacyExtendedURLTemporaryResourceReferenceSerializer
    extends ExtendedURLTemporaryResourceReferenceSerializer
{
}
