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
package org.xwiki.rendering.signature.internal;

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.crypto.signer.CMSSignedDataGenerator;
import org.xwiki.crypto.signer.param.CMSSignedDataGeneratorParameters;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.MacroMarkerBlock;
import org.xwiki.rendering.signature.BlockSignatureGenerator;

/**
 * Sign {@link MacroBlock} and {@link MacroMarkerBlock}.
 *
 * @version $Id$
 * @since 6.1M2
 */
@Component
@Named("macro")
@Singleton
public class MacroBlockSignatureGenerator implements BlockSignatureGenerator
{
    @Inject
    @Named("macro")
    private BlockDumper dumper;

    @Inject
    private CMSSignedDataGenerator generator;

    @Override
    public byte[] generate(Block block, CMSSignedDataGeneratorParameters params)
        throws GeneralSecurityException, IOException
    {
        if (!isSupported(block)) {
            throw new IllegalArgumentException("Unsupported block [" + block.getClass().getName() + "].");
        }

        return generator.generate(dumper.dump(block), params);
    }

    @Override
    public boolean isSupported(Block block)
    {
        return (block instanceof MacroBlock || block instanceof MacroMarkerBlock);
    }

}
