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
package org.xwiki.livedata;

import org.xwiki.livedata.internal.JSONMerge;
import org.xwiki.stability.Unstable;

/**
 * Abstract implementation of {@link LiveDataConfigurationResolver} taking a {@link LiveDataConfiguration}.
 * This implementation ensure to resolve the configuration by performing a merge with the given configuration,
 * after first setting properly the identifier for having a proper merge.
 *
 * @version $Id$
 * @since 17.7.0RC1
 * @since 17.4.4
 * @since 16.10.11
 */
@Unstable
public abstract class AbstractLiveDataConfigurationResolver implements
    LiveDataConfigurationResolver<LiveDataConfiguration>
{
    /**
     * Used to merge the default configuration with the provided configuration.
     */
    protected JSONMerge jsonMerge = new JSONMerge();

    /**
     * Retrieve the default configuration to use for performing the merge.
     * @param input the input if needed to get the default configuration.
     * @return the configuration to use for performing the merge.
     * @throws LiveDataException in case of problem to retrieve the configuration.
     */
    protected abstract LiveDataConfiguration getDefaultConfiguration(LiveDataConfiguration input)
        throws LiveDataException;

    @Override
    public LiveDataConfiguration resolve(LiveDataConfiguration input) throws LiveDataException
    {
        LiveDataConfiguration defaultConfiguration = getDefaultConfiguration(input);
        defaultConfiguration.setId(input.getId());

        return this.jsonMerge.merge(defaultConfiguration, input);
    }
}
