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
package org.xwiki.model.reference.internal;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.crypto.BinaryStringEncoder;
import org.xwiki.crypto.Digest;
import org.xwiki.crypto.DigestFactory;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.BlockReference;
import org.xwiki.model.reference.BlockReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.signature.internal.BlockDumper;

/**
 * Resolve macro block references from {@link Block} instances for the purpose of linking them to signatures.
 *
 * The name of the block reference is an SHA-1 digest based on the macro block content, parameters, and the
 * metadata source if found in XDOM. The block receive no parent by default. A optional
 * {@link EntityReference} can be provided to specify the parent entity.
 *
 * @version $Id$
 * @since 6.1M2
 */
@Component
@Named("signedmacro")
@Singleton
public class SignedMacroBlockReferenceResolver implements BlockReferenceResolver<Block>
{
    @Inject
    @Named("SHA-1")
    private DigestFactory digestFactory;

    @Inject
    @Named("Base64")
    private BinaryStringEncoder encoder;

    @Inject
    @Named("macro")
    private BlockDumper dumper;

    /**
     * {@inheritDoc}
     *
     * This implementation is specific for macro blocks. The block argument could be either
     * a {@link org.xwiki.rendering.block.MacroBlock} or a {@link org.xwiki.rendering.block.MacroMarkerBlock}.
     */
    @Override
    public BlockReference resolve(Block block, Object... parameters)
    {
        EntityReference parent = null;

        if (parameters.length > 0 && parameters[0] instanceof EntityReference) {
            // Try to extract the type from the passed parameter.
            parent = (EntityReference) parameters[0];
        }

        Digest digest = digestFactory.getInstance();

        try {
            dumper.dump(digest.getOutputStream(), block);
            return new BlockReference(new EntityReference(encoder.encode(digest.digest()), EntityType.BLOCK, parent));
        } catch (IOException ignore) {
            // Ignored
        }
        return null;
    }
}
