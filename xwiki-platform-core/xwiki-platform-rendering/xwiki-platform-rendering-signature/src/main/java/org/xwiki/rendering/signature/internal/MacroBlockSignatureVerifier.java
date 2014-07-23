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
import org.xwiki.crypto.pkix.CertificateProvider;
import org.xwiki.crypto.signer.CMSSignedDataVerifier;
import org.xwiki.crypto.signer.param.CMSSignedDataVerified;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.MacroMarkerBlock;
import org.xwiki.rendering.signature.BlockSignatureVerifier;

/**
 * Verify signature of {@link org.xwiki.rendering.block.MacroBlock}
 * and {@link org.xwiki.rendering.block.MacroMarkerBlock}.
 *
 * @version $Id$
 * @since 6.1M2
 */
@Component
@Named("macro")
@Singleton
public class MacroBlockSignatureVerifier implements BlockSignatureVerifier
{
    @Inject
    @Named("macro")
    private BlockDumper dumper;

    @Inject
    private CMSSignedDataVerifier verifier;

    @Override
    public CMSSignedDataVerified verify(byte[] signature, Block block, CertificateProvider certificateProvider)
        throws GeneralSecurityException, IOException
    {
        if (!isSupported(block)) {
            throw new IllegalArgumentException("Unsupported block [" + block.getClass().getName() + "].");
        }

        return verifier.verify(signature, dumper.dump(block), certificateProvider);
    }

    @Override
    public boolean isSupported(Block block)
    {
        return (block instanceof MacroBlock || block instanceof MacroMarkerBlock);
    }

}

