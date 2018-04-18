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
package org.xwiki.xar.internal.type;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.xar.XarEntryTypeResolver;
import org.xwiki.xar.type.AbstractXarEntryType;

/**
 * Pre-configured type for wiki home page use case.
 * 
 * @version $Id$
 * @since 10.3
 */
@Component(hints = { HomeXarEntryType.HINT, XarEntryTypeResolver.DOCUMENT_PREFIX + "Main.WebHome" })
@Singleton
public class HomeXarEntryType extends AbstractXarEntryType
{
    /**
     * The name of the type.
     */
    public static final String HINT = "home";

    /**
     * Default constructor.
     */
    public HomeXarEntryType()
    {
        super(HINT);

        // It's OK to customize the home page, it's even expected
        setEditAllowed(true);

        // You only want to upgrade if you kept the standard home page
        setUpgradeType(UpgradeType.SKIP);
    }
}
