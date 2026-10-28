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

import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.livetable.LiveTableNewRowNamingStrategy;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.XWikiException;

/**
 * {@link LiveTableNewRowNamingStrategy} that generates a page in a configured location, named using a UUID.
 *
 * @version $Id$
 * @since 18.7.0RC1
 */
@Component
@Named("uuid")
@Singleton
@Unstable
public class UUIDLiveTableNewRowNamingStrategy extends AbstractLocationLiveTableNewRowNamingStrategy
{
    @Inject
    private ModelBridge modelBridge;

    @Override
    public DocumentReference generate(Map<String, Object> parameters, Map<String, Object> entryValues)
        throws LiveDataException, XWikiException
    {
        DocumentReference candidate =
            new DocumentReference(UUID.randomUUID().toString(), getRequiredLocation(parameters));
        if (this.modelBridge.exists(candidate)) {
            throw new LiveDataException(String.format("The page [%s] already exists.", candidate));
        }
        return candidate;
    }
}
