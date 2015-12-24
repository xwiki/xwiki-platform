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
package org.xwiki.rendering.signature;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.xwiki.component.annotation.Role;
import org.xwiki.crypto.signer.param.CMSSignedDataGeneratorParameters;
import org.xwiki.rendering.block.Block;

/**
 * Generate the signature of a {@link Block}.
 *
 * @version $Id$
 * @since 6.1M2
 */
@Role
public interface BlockSignatureGenerator
{
    /**
     * Generate a signature.
     *
     * @param block a rendering block to sign.
     * @param params generator parameters.
     * @return a Base64 encoded signature.
     * @throws GeneralSecurityException on signature operation error.
     * @throws IOException on encoding error.
     */
    byte[] generate(Block block, CMSSignedDataGeneratorParameters params)
        throws GeneralSecurityException, IOException;

    /**
     * Check if the given block can be supported by this signer.
     *
     * @param block the block to check.
     * @return true if this block can be signed/verified by this signer.
     */
    boolean isSupported(Block block);
}
