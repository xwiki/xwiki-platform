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
package org.xwiki.rendering.macro;

import org.xwiki.crypto.pkix.CertificateProvider;
import org.xwiki.crypto.signer.param.CMSSignedDataGeneratorParameters;
import org.xwiki.crypto.signer.param.CMSSignedDataVerified;
import org.xwiki.rendering.block.Block;

/**
 * Mixin interface providing additional signature functionality to a macro.
 *
 * @version $Id$
 * @since 6.1M2
 */
public interface SignableMacro
{
    /**
     * Sign the given macro block.
     *
     * @param block the block to sign. This block should have a known content source and could be either
     *              a {@link org.xwiki.rendering.block.MacroBlock} or
     *              a {@link org.xwiki.rendering.block.MacroMarkerBlock}.
     * @param parameters the signature generation parameters.
     * @throws MacroSignatureException on error.
     */
    void sign(Block block, CMSSignedDataGeneratorParameters parameters) throws MacroSignatureException;

    /**
     * Verify signature of the given macro block.
     *
     * @param block the block to verify.  This block should have a known content source and could be either
     *              a {@link org.xwiki.rendering.block.MacroBlock} or
     *              a {@link org.xwiki.rendering.block.MacroMarkerBlock}.
     * @param certificateProvider a certificate provider providing available certificates.
     * @return signature verification results, or null no signature where found for the given block.
     * @throws MacroSignatureException on error.
     */
    CMSSignedDataVerified verify(Block block, CertificateProvider certificateProvider)
        throws MacroSignatureException;
}
