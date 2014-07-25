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
package org.xwiki.wikistream.descriptor;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.xwiki.stability.Unstable;

/**
 * @version $Id$
 * @since 5.3M2
 */
@Unstable
public abstract class AbstractWikiStreamDescriptor implements WikiStreamDescriptor
{
    /**
     * @see #getName()
     */
    protected String name;

    /**
     * The description of the macro.
     */
    protected String description;

    /**
     * A map containing the {@link WikiStreamPropertyDescriptor} for each parameters supported for this wiki stream.
     * <p>
     * The {@link Map} keys are lower cased for easier case insensitive search, to get the "real" name of the property
     * use {@link WikiStreamPropertyDescriptor#getName()}.
     */
    protected Map<String, WikiStreamPropertyDescriptor< ? >> parameterDescriptorMap =
        new LinkedHashMap<String, WikiStreamPropertyDescriptor< ? >>();

    /**
     * @param name human readable name of wiki stream input source type.
     * @param description the description of the wiki stream
     */
    public AbstractWikiStreamDescriptor(String name, String description)
    {
        this.name = name;
        this.description = description;
    }

    // WikiStreamDescriptor

    @Override
    public String getName()
    {
        return this.name;
    }

    @Override
    public String getDescription()
    {
        return this.description;
    }

    @Override
    public <T> WikiStreamPropertyDescriptor<T> getPropertyDescriptor(String propertyName)
    {
        return (WikiStreamPropertyDescriptor<T>) this.parameterDescriptorMap.get(propertyName);
    }

    @Override
    public Collection<WikiStreamPropertyDescriptor< ? >> getProperties()
    {
        return Collections.<WikiStreamPropertyDescriptor< ? >> unmodifiableCollection(this.parameterDescriptorMap
            .values());
    }
}
