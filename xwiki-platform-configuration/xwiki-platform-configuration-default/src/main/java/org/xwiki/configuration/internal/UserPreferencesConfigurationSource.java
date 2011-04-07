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
package org.xwiki.configuration.internal;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;

/**
 * Configuration source taking its data in the User Preferences wiki document
 * (the user profile page) using data from a XWikiUsers object attached
 * to that document. 
 *  
 * @version $Id$
 * @since 2.0M2
 */
@Component("user")
public class UserPreferencesConfigurationSource  extends AbstractDocumentConfigurationSource
{
    @Override
    protected DocumentReference getClassReference()
    {
        // TODO: Not enabled yet. See #getDocumentReference().
        return null;
    }

    @Override
    protected DocumentReference getDocumentReference()
    {
        // TODO: Not enabled yet. In order to enable it we need to make modifications so that
        // DAB.getCurrentUser() returns a DocumentReference and not a String as otherwise it will create
        // a stackoverflow (circular dependency): in order to create a DocumentReference we would need to
        // use a factory which would need to use this configuration source.
        return null;
    }
}
