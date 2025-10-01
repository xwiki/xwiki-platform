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
package org.xwiki.edit.internal;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.configuration.internal.AbstractSpacesConfigurationSource;

/**
 * Configuration Source that looks for editor bindings in the space preferences page for each of the ancestor spaces of
 * the current document, starting from its parent space and moving upwards.
 * 
 * @version $Id$
 * @since 8.2RC1
 */
@Component
@Named("editorBindings/spaces")
@Singleton
public class SpacesEditorBindingsSource extends AbstractSpacesConfigurationSource
{
    @Inject
    @Named("editorBindings/space")
    private ConfigurationSource spaceEditorBindingsSource;

    @Override
    protected ConfigurationSource getSpaceConfigurationSource()
    {
        return this.spaceEditorBindingsSource;
    }
}
