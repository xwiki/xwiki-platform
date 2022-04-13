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
package org.xwiki.rendering.internal.transformation.macro;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MetaDataBlock;
import org.xwiki.rendering.block.match.MetadataBlockMatcher;
import org.xwiki.rendering.listener.MetaData;

/**
 * Resolves a String reference, usually passed as a Macro parameter, into a {@link EntityReference}, using any
 * {@link MetaDataBlock} with a {@link MetaData#BASE} setting to resolve the any relative reference parts into a fully
 * resolved object.
 * 
 * @version $Id$
 * @since 5.0M1
 */
@Component
@Named("macro")
@Singleton
public class CurrentMacroEntityReferenceResolver implements EntityReferenceResolver<String>
{
    /**
     * Used to resolve references.
     */
    @Inject
    @Named("current")
    private EntityReferenceResolver<String> currentEntityReferenceResolver;

    @Override
    public EntityReference resolve(String representation, EntityType entityType, Object... parameters)
    {
        // There must be at least 1 parameter and the first parameter must be of type Block
        if (parameters.length < 1 || !(parameters[0] instanceof Block)) {
            throw new IllegalArgumentException(String.format("There must be at least one parameter, with the first "
                + "parameter of type [%s]", Block.class.getName()));
        }

        Block currentBlock = (Block) parameters[0];

        EntityReference result;

        MetaDataBlock metaDataBlock =
            currentBlock.getFirstBlock(new MetadataBlockMatcher(MetaData.BASE), Block.Axes.ANCESTOR);

        // If no Source MetaData was found resolve against the current entity as a failsafe solution.
        if (metaDataBlock == null) {
            result = resolveWhenNoBASEMetaData(representation, entityType, parameters);
        } else {
            String sourceMetaData = (String) metaDataBlock.getMetaData().getMetaData(MetaData.BASE);
            result =
                this.currentEntityReferenceResolver.resolve(representation, entityType,
                    this.currentEntityReferenceResolver.resolve(sourceMetaData, EntityType.DOCUMENT));
        }

        return result;
    }

    private EntityReference resolveWhenNoBASEMetaData(String representation, EntityType entityType,
        Object... parameters)
    {
        EntityReference result;

        // Rebuild a parameter list without the first one, to be able to pass it to the current resolver and
        // thus support passing an Entity Reference for example.
        if (parameters.length > 1) {
            Object[] newParameters = new Object[parameters.length - 1];
            System.arraycopy(parameters, 1, newParameters, 0, parameters.length - 1);
            result = this.currentEntityReferenceResolver.resolve(representation, entityType, newParameters);
        } else {
            result = this.currentEntityReferenceResolver.resolve(representation, entityType);
        }

        return result;
    }
}
