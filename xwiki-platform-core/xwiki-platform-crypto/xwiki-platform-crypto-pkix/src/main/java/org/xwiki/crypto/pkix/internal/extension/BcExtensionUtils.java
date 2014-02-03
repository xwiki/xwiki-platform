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

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.xwiki.crypto.pkix.params.x509certificate.extension.ExtendedKeyUsages;
import org.xwiki.crypto.pkix.params.x509certificate.extension.KeyUsage;
import org.xwiki.crypto.pkix.params.x509certificate.extension.X509DirectoryName;
import org.xwiki.crypto.pkix.params.x509certificate.extension.X509DnsName;
import org.xwiki.crypto.pkix.params.x509certificate.extension.X509GeneralName;
import org.xwiki.crypto.pkix.params.x509certificate.extension.X509GenericName;
import org.xwiki.crypto.pkix.params.x509certificate.extension.X509IpAddress;
import org.xwiki.crypto.pkix.params.x509certificate.extension.X509Rfc822Name;
import org.xwiki.crypto.pkix.params.x509certificate.extension.X509URI;

/**
 * Utility class for converting extension from/into Bouncy Castle equivalents.
 *
 * @version $Id$
 * @since 5.4
 */
public final class BcExtensionUtils
{
    private BcExtensionUtils()
    {
        // Utility class.
    }

    /**
     * Convert general names from Bouncy Castle general names.
     *
     * @param genNames Bouncy castle general names.
     * @return a list of X.509 general names.
     */
    public static List<X509GeneralName> getX509GeneralNames(GeneralNames genNames)
    {
        if (genNames == null) {
            return null;
        }

        GeneralName[] names = genNames.getNames();
        List<X509GeneralName> x509names = new ArrayList<X509GeneralName>(names.length);

        for (GeneralName name : names) {
            switch (name.getTagNo()) {
                case GeneralName.rfc822Name:
                    x509names.add(new X509Rfc822Name(name));
                    break;
                case GeneralName.dNSName:
                    x509names.add(new X509DnsName(name));
                    break;
                case GeneralName.directoryName:
                    x509names.add(new X509DirectoryName(name));
                    break;
                case GeneralName.uniformResourceIdentifier:
                    x509names.add(new X509URI(name));
                    break;
                case GeneralName.iPAddress:
                    x509names.add(new X509IpAddress(name));
                    break;
                default:
                    x509names.add(new X509GenericName(name));
                    break;
            }
        }

        return x509names;
    }

    /**
     * Convert usages from Bouncy Castle.
     *
     * @param keyUsage the bouncy castle key usage to convert.
     * @return the set of authorized usages.
     */
    public static EnumSet<KeyUsage> getSetOfKeyUsage(org.bouncycastle.asn1.x509.KeyUsage keyUsage)
    {
        if (keyUsage == null) {
            return null;
        }

        Collection<KeyUsage> usages = new ArrayList<KeyUsage>();

        for (KeyUsage usage : KeyUsage.values()) {
            if ((((DERBitString) keyUsage.toASN1Primitive()).intValue() & usage.value()) > 0) {
                usages.add(usage);
            }
        }
        return EnumSet.copyOf(usages);
    }

    /**
     * Convert extended usages from Bouncy Castle.
     *
     * @param usages the bouncy castle extended key usage to convert.
     * @return the set of authorized usages.
     */
    public static ExtendedKeyUsages getExtendedKeyUsages(ExtendedKeyUsage usages)
    {
        if (usages == null) {
            return null;
        }

        List<String> usageStr = new ArrayList<String>();

        for (KeyPurposeId keyPurposeId : usages.getUsages()) {
            usageStr.add(keyPurposeId.getId());
        }

        return new ExtendedKeyUsages(usageStr);
    }

    /**
     * Convert a collection of X.509 general names to Bouncy Castle general names.
     *
     * @param genNames a collection of X.509 general names.
     * @return a bouncy castle general names.
     */
    public static GeneralNames getGeneralNames(X509GeneralName[] genNames)
    {
        GeneralName[] names = new GeneralName[genNames.length];

        int i = 0;
        for (X509GeneralName name : genNames) {
            if (name instanceof BcGeneralName) {
                names[i++] = ((BcGeneralName) name).getGeneralName();
            } else {
                throw new IllegalArgumentException("Unexpected general name: " + name.getClass().toString());
            }
        }

        return new GeneralNames(names);
    }

    /**
     * Convert a set of key usages to Bouncy Castle key usage.
     *
     * @param usages the set of authorized usages.
     * @return a bit mask
     */
    public static org.bouncycastle.asn1.x509.KeyUsage getKeyUsage(EnumSet<KeyUsage> usages)
    {
        int bitmask = 0;
        for (KeyUsage usage : usages) {
            bitmask |= usage.value();
        }
        return new org.bouncycastle.asn1.x509.KeyUsage(bitmask);
    }

    /**
     * Convert a set of extended key usages to Bouncy Castle extended key usage.
     *
     * @param usages the set of authorized usages.
     * @return a bit mask
     */
    public static ExtendedKeyUsage getExtendedKeyUsage(Set<String> usages)
    {
        KeyPurposeId[] keyUsages = new KeyPurposeId[usages.size()];
        int i = 0;

        for (String usage : usages) {
            keyUsages[i++] = KeyPurposeId.getInstance(new ASN1ObjectIdentifier(usage));
        }

        return new ExtendedKeyUsage(keyUsages);
    }
}
