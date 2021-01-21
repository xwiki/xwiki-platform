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
package org.xwiki.livedata.internal.livetable;

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.livedata.LiveDataConfiguration;
import org.xwiki.livedata.LiveDataConfigurationResolver;
import org.xwiki.livedata.LiveDataException;

/**
 * Provides the default live data configuration for the live table source.
 * 
 * @version $Id$
 * @since 12.10.4
 * @since 13.0
 */
@Component
@Named("liveTable")
@Singleton
public class DefaultLiveDataConfigurationProvider implements Provider<LiveDataConfiguration>
{
    /**
     * Used to parse the default configuration JSON.
     */
    @Inject
    private LiveDataConfigurationResolver<String> stringLiveDataConfigResolver;

    /**
     * Cache the static default configuration JSON.
     */
    private String defaultConfigJSON;

    @Override
    public LiveDataConfiguration get()
    {
        if (this.defaultConfigJSON == null) {
            try {
                InputStream defaultConfigInputStream =
                    getClass().getResourceAsStream("/liveTableLiveDataConfiguration.json");
                this.defaultConfigJSON = IOUtils.toString(defaultConfigInputStream, "UTF-8");
            } catch (IOException e) {
                throw new RuntimeException(
                    "Failed to read the default live data configuration for the live table source.", e);
            }
        }

        try {
            return this.stringLiveDataConfigResolver.resolve(this.defaultConfigJSON);
        } catch (LiveDataException e) {
            throw new RuntimeException("Failed to parse the default live data configuration for the live table source.",
                e);
        }
    }
}
