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
package org.xwiki.crypto.password.internal.pbe.factory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.crypto.cipher.CipherFactory;

/**
 * Implement PBES2 encryption scheme with RC5 encryption with 64 bits block.
 *
 * WARNING: RC5 is protected by U.S. Patents 5,724,428 and 5,835,600. Therefore, before expiration of these patents,
 * the usage of this algorithm is subject to restricted usage on the US territories.
 * RC5 is a trademark of RSA Security Inc.
 *
 * @version $Id$
 * @since 5.4M1
 */
@Component(hints = { "PBES2-RC5-32-CBC-Pad", "1.2.840.113549.3.9" })
@Singleton
public class BcPBES2Rc5b64CipherFactory extends AbstractBcPBES2Rc5CipherFactory
{
    @Inject
    @Named("RC5-32/CBC/PKCS5Padding")
    private CipherFactory cipherFactory;

    @Override
    protected CipherFactory getCipherFactory()
    {
        return cipherFactory;
    }
}
