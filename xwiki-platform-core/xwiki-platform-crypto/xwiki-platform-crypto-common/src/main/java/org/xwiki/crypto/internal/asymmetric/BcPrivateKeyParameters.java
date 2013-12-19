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
package org.xwiki.crypto.internal.asymmetric;

import java.io.IOException;

import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.PrivateKeyInfoFactory;
import org.xwiki.crypto.params.cipher.asymmetric.PrivateKeyParameters;

/**
 * Encapsulate a Bouncy Castle asymmetric parameter of a private key.
 *
 * @version $Id$
 * @since 5.4M1
 */
public class BcPrivateKeyParameters extends AbstractBcAsymmetricKeyParameters
                                    implements PrivateKeyParameters
{
    /**
     * Encapsulate a given Bouncy Castle private key parameter.
     *
     * @param parameters a BC private key parameter.
     */
    public BcPrivateKeyParameters(AsymmetricKeyParameter parameters)
    {
        super(parameters);
        if (!isPrivate()) {
            throw new IllegalArgumentException("Non-private key assigned to a private key: "
                + parameters.getClass().getName());
        }
    }

    @Override
    public byte[] getEncoded()
    {
        try {
            return PrivateKeyInfoFactory.createPrivateKeyInfo(parameters).getEncoded();
        } catch (IOException e) {
            return null;
        }
    }
}
