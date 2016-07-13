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
package org.xwiki.lesscss.internal.resources;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.lesscss.resources.LESSResourceReference;
import org.xwiki.lesscss.resources.LESSResourceReferenceFactory;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.skin.SkinManager;
import org.xwiki.template.TemplateManager;

/**
 * Default implementation for {@link org.xwiki.lesscss.resources.LESSResourceReferenceFactory}.
 * 
 * @since 7.0RC1
 * @version $Id$
 */
@Component
@Singleton
public class DefaultLESSResourceReferenceFactory implements LESSResourceReferenceFactory
{
    @Inject
    private TemplateManager templateManager;

    @Inject
    private SkinManager skinManager;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    private DocumentAccessBridge bridge;
    
    @Override
    public LESSResourceReference createReferenceForSkinFile(String fileName)
    {
        return new LESSSkinFileResourceReference(fileName, templateManager, skinManager);
    }

    @Override
    public LESSResourceReference createReferenceForXObjectProperty(ObjectPropertyReference objectPropertyReference)
    {
        return new LESSObjectPropertyResourceReference(objectPropertyReference, entityReferenceSerializer, bridge);
    }
}
