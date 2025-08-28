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
package org.xwiki.extension.security.internal.livedata;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.livedata.LiveDataEntryStore;
import org.xwiki.livedata.LiveDataPropertyDescriptorStore;
import org.xwiki.livedata.LiveDataSource;

/**
 * Live Data source for the Extensions Security Vulnerabilities Application, listing all vulnerabilities.
 *
 * @version $Id$
 * @since 15.5RC1
 */
@Component
@Singleton
@Named(ExtensionSecurityLiveDataSource.ID)
public class ExtensionSecurityLiveDataSource implements LiveDataSource
{
    /**
     * The extension security live data source id.
     */
    public static final String ID = "extensionSecurity";

    @Inject
    @Named(ID)
    private LiveDataEntryStore entryStore;

    @Inject
    @Named(ID)
    private LiveDataPropertyDescriptorStore propertyStore;

    @Override
    public LiveDataEntryStore getEntries()
    {
        return this.entryStore;
    }

    @Override
    public LiveDataPropertyDescriptorStore getProperties()
    {
        return this.propertyStore;
    }
}
