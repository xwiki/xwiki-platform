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
package org.xwiki.crypto.pkix.internal.extension;

import java.io.IOException;
import java.util.Enumeration;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.ExtensionsGenerator;
import org.xwiki.crypto.pkix.X509ExtensionBuilder;
import org.xwiki.crypto.pkix.params.x509certificate.extension.X509Extensions;

/**
 * Base class for an extension builder based on Bouncy Castle.
 *
 * @version $Id$
 * @since 5.4
 */
public abstract class AbstractBcX509ExtensionBuilder implements X509ExtensionBuilder
{
    private final ExtensionsGenerator extensions = new ExtensionsGenerator();

    @Override
    public X509ExtensionBuilder addExtension(String oid, boolean critical, byte[] value) throws IOException
    {
        extensions.addExtension(new ASN1ObjectIdentifier(oid), critical, value);
        return this;
    }

    @Override
    public X509ExtensionBuilder addExtensions(X509Extensions extensionSet) throws IOException
    {
        if (extensionSet == null) {
            return this;
        }

        // Optimisation
        if (extensionSet instanceof BcX509Extensions) {
            Extensions exts = ((BcX509Extensions) extensionSet).getExtensions();
            @SuppressWarnings("unchecked")
            Enumeration<ASN1ObjectIdentifier> oids = exts.oids();
            while (oids.hasMoreElements()) {
                ASN1ObjectIdentifier oid = oids.nextElement();
                Extension ext = exts.getExtension(oid);
                extensions.addExtension(ext.getExtnId(), ext.isCritical(), ext.getParsedValue());
            }
        } else {
            // Fallback
            for (String oid : extensionSet.getExtensionOID()) {
                extensions.addExtension(new ASN1ObjectIdentifier(oid), extensionSet.isCritical(oid),
                    extensionSet.getExtensionValue(oid));
            }
        }
        return this;
    }

    /**
     * Add an extension.
     *
     * @param oid the extension oid.
     * @param critical true if the extension is critical.
     * @param value the value of the extension.
     * @return this extensions builder to allow chaining.
     */
    public X509ExtensionBuilder addExtension(ASN1ObjectIdentifier oid, boolean critical, ASN1Encodable value)
    {
        try {
            extensions.addExtension(oid, critical, value.toASN1Primitive().getEncoded(ASN1Encoding.DER));
        } catch (IOException e) {
            // Very unlikely
            throw new IllegalArgumentException("Invalid extension value, it could not be properly DER encoded.");
        }
        return this;
    }

    @Override
    public X509Extensions build()
    {
        if (extensions.isEmpty()) {
            return null;
        }
        return new BcX509Extensions(extensions.generate());
    }

    @Override
    public boolean isEmpty()
    {
        return extensions.isEmpty();
    }
}
