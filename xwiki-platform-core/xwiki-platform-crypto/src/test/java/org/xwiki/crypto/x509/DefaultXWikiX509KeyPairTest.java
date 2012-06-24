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
package org.xwiki.crypto.x509;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.crypto.passwd.PasswordCryptoService;
import org.xwiki.crypto.x509.internal.DefaultXWikiX509KeyPair;
import org.xwiki.test.AbstractComponentTestCase;


/**
 * Tests the {@link DefaultXWikiX509KeyPair} implementation.
 * 
 * @version $Id$
 * @since 2.5M1
 */
public class DefaultXWikiX509KeyPairTest extends AbstractComponentTestCase
{
    private final String password = "blah";

    private final String keyPairExportedAsBase64 =
        "-----BEGIN XWIKI CERTIFICATE AND PRIVATE KEY-----\n"
      + "rO0ABXNyADZvcmcueHdpa2kuY3J5cHRvLng1MDkuaW50ZXJuYWwuRGVmYXVsdFhXaWtpWDUwOUtleVBhaXIAAAAAAAAAAQIAA1sAEmVuY29kZ\n"
      + "WRDZXJ0aWZpY2F0ZXQAAltCWwAbcGFzc3dvcmRFbmNyeXB0ZWRQcml2YXRlS2V5cQB+AAFMABNwcml2YXRlS2V5QWxnb3JpdGhtdAASTGphdm\n"
      + "EvbGFuZy9TdHJpbmc7eHB1cgACW0Ks8xf4BghU4AIAAHhwAAADPDCCAzgwggIgoAMCAQICBgEqOHiOzzANBgkqhkiG9w0BAQUFADAgMR4wHAY\n"
      + "KCZImiZPyLGQBAQwOeHdpa2k6WFdpa2kuTWUwHhcNMTAwODAzMTQwMTE5WhcNMTAwODA0MTUwMTE5WjAgMR4wHAYKCZImiZPyLGQBAQwOeHdp\n"
      + "a2k6WFdpa2kuTWUwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQC3RGX6ajWNVfFM+FatwqTqvZ09jOu9fpddE1gSY1QfSeztpTtPj\n"
      + "ShUDzzPHqytm+Rdh1yQXg6C3aZkIs6QlLRTYIfhLhxiPpXX9YvgB6vPgtew9vILXmKO9SH7Gb8td4PxsjS09G5KIaCdzeA43dD1xnjUCa2mRJ\n"
      + "4pMCqx2NXKeQg69s/p/bXA1z6iQBRsDr3iRTLs5mCgJFyEBNJ5T/uDbaV1pgP36ocFgttE1bSi2ZN/5sDRZ3+kQWnnPOSmjwWZXjcwdQd/9+7\n"
      + "XQ29xDSoPm3sZHppKjqpJZuukj41NfHFxf0Q1xIm1aVCJ1yvmw4tUejqCffG1o9nt76exVjCJAgMBAAGjeDB2MAwGA1UdEwEB/wQCMAAwEQYJ\n"
      + "YIZIAYb4QgEBBAQDAgWgMA4GA1UdDwEB/wQEAwIDuDAfBgNVHSMEGDAWgBRTQDKhMfzjIYrqOzmL4dvxWia4UDAiBgNVHREBAf8EGDAWhhRod\n"
      + "HRwOi8vd3d3Lm15LndlYi5pZDANBgkqhkiG9w0BAQUFAAOCAQEAtUm923h5w/TP2f0kR1dYvip+qE095jlGFMbwQsxUR4PRsx9S6RhNAsZTvo\n"
      + "4aJ/5TbTKgb1893TcUCeziZBb9QSNSYixxh67fVZrpY45XuQo/+j1FA9ugDr/A3oIfrsl6TkbJ3a4EaXBhAWNsM9OXanr04zsJyjwxS/xw9ll\n"
      + "WOFR0ASLh7Mfm/1RMo5Bk8O8+h5sJCKg7VgX1+6REiXm9Rbd7yymB9n0qlC0YlCwe3/XubJOtW59XKdmDiRCTt71wYnAtemxlb4r4ku8ZMHen\n"
      + "RKDIvJ0wR0hRAtM5CDu0asUPgTtLbPjIHlCMTedNS9kAUfCYg9LkdGW0o65eRw2BkHVxAH4ABAAABu6s7QAFc3IAOG9yZy54d2lraS5jcnlwd\n"
      + "G8ucGFzc3dkLmludGVybmFsLkNBU1Q1UGFzc3dvcmRDaXBoZXJ0ZXh0AAAAAAAAAAECAAB4cgA7b3JnLnh3aWtpLmNyeXB0by5wYXNzd2QuaW\n"
      + "50ZXJuYWwuQWJzdHJhY3RQYXNzd29yZENpcGhlcnRleHQAAAAAAAAAAQIAAlsACmNpcGhlcnRleHR0AAJbQkwAC2tleUZ1bmN0aW9udAAvTG9\n"
      + "yZy94d2lraS9jcnlwdG8vcGFzc3dkL0tleURlcml2YXRpb25GdW5jdGlvbjt4cHVyAAJbQqzzF/gGCFTgAgAAeHAAAATIPlEN+c4WU4v8z7lJ\n"
      + "NqKvWIBdgXd9emPLStRgdkOQyKACVVd2NDwDek3N5TBEhD1wCUdIoTk+F8bYz3XFb2hfvQNibrhQJfczckFi8PRM+j0ETAueFe2ZSlciR/x9n\n"
      + "En0EyB+hDbmSScHonjIeQQJju5lpT8zB2HGCPqAR1x2i7wXzkK+8QX7sVHOJq+qxDWH3+Qmp445Rox6irmqjobr/cTmFq4ASc+5wQr2FlGEW4\n"
      + "eG5c3OwCDA+qRzjZmzvThuzGkqajOK4etHHTu4ZjL+Vbx3Y3rLLr+55NY1MBdqgqM6/UdGZCyo3D+Hb9wTnOZq+v9ElMIEjef0moPAWPm7au1\n"
      + "eFfBh1Cgsg6KNLK8iQhxAxVBa6tYKaxtIkGmeOwdqlD9APuyR6pWD7P0JhqHrOd+0OlHCUxWCVuQPpv7mEmvITeQ88Zhid8kRhGMr6wUGi9eG\n"
      + "KLIw02lp0DWiZeTi++eq9OtaLYM3kG3ldD7Bn7qjbYw3+2kiWb2oUDJgVzeHTrb5EZareEkNmD86/1k1Y/2zXyMZ1MG8QetizU+ospUayIBXr\n"
      + "U/tsxX5DJ5Z23mOzUbhieSB5gSGQXmvEh4S6ajE/6SzPF9eLy3gGLzaEeUqFi/khhPxVBCD1wGf/yLFVpWU8GgAGJlM5g/VbkYmyrB3N5I6W7\n"
      + "eH7xr/3uE8UaNRQ/wMNlTh68ES/Da/mMtXlKxrRPQM/jCKd36dguVdVBDzDaeJwDadZtpZpzoyZemx1Jt2IfEAkTG7orM9BeYYq4ZtIpSJ+of\n"
      + "S7xy0C+21MO0t+47OSyh7o3ugDL7k6p0BkMwSHZhb6mPHCSvNRI99HS9GQxnPK3ZIu6a3Y2krWIDS0guEqUHwYywh6h9y5s6dIHTn+G33S+ns\n"
      + "aczyVRCZ86kg+sinEyqAsIZEAqLP/OEeuJtSENdO7L3HoWTuy7yzkxxR/Fbfa3KBTkXMPTkd0bg6pjJ8HUjif+qM3zvN+KN89qESaYZB+32lX\n"
      + "L7qyBvloajQMC26dZFR0X0N2ynKRWKUaF18uRR5oQ/Cs4aCmrBkyBaP1IyyHr1P8VtIaKPwXSfLltRWW2ngZk3+ZNWxwYBnah2hPiAESpTgSI\n"
      + "HJDFPdYKljErlbZXT7LLD/8+I2tNRii9i3GO3aSdFtvrN/Ki8iliM64A3lrgIBLaku921I6oRLSOpwXIDp/KUnavhUgnvdqr5TCoHTIyFhC8B\n"
      + "Tr5sYB7kwEyympsB+UT/79msIoubVK8IKYzq2akrwDWQjpTl3RTrX8s8MdvW3Ain3q1iPCDlcW2Tx2/0bNIJ4uRpCXvqKVS7AxlpxZBviBSuS\n"
      + "3gucQfiFCPhKTCnTDuyPX4egBYPwuvFabAJWdimTKXKpXRNyPA7oAuTc/HQavcArC37YboRf6KgoMvSlrl+aWT8mBuiA+QwwTpWwOCe/xu1Ri\n"
      + "/F9e1zA6UiPvhCB9JpnRhgJ9P21cndnsBP9l43Pq4MzZDG7sjPGuOk9RtekumeFSy1iXzIYmpaP1Ip55sTuL6gLgMhYLc5+vLFCpIYj/0PHRJ\n"
      + "sI5Bl1E6k0GsdV/bMO8AEU2YFEoFxA+TYbeyP8d22i/NjAq09KGNE9Qjs/nxwRzavQyW+/PL7z4rC6oT/Smqs6qQ6mc3IARm9yZy54d2lraS5\n"
      + "jcnlwdG8ucGFzc3dkLmludGVybmFsLlNjcnlwdE1lbW9yeUhhcmRLZXlEZXJpdmF0aW9uRnVuY3Rpb24AAAAAAAAAAQIABUkACWJsb2NrU2l6\n"
      + "ZUkAEGRlcml2ZWRLZXlMZW5ndGhJAA1tZW1vcnlFeHBlbnNlSQAQcHJvY2Vzc29yRXhwZW5zZVsABHNhbHRxAH4AAnhyAEhvcmcueHdpa2kuY\n"
      + "3J5cHRvLnBhc3N3ZC5pbnRlcm5hbC5BYnN0cmFjdE1lbW9yeUhhcmRLZXlEZXJpdmF0aW9uRnVuY3Rpb24AAAAAAAAAAQIAAHhwAAAACAAAAB\n"
      + "gAAAQAAAAACHVxAH4ABQAAABDJ7hrb6YGgV4L/F3hlyE2tdAADUlNB\n"
      + "-----END XWIKI CERTIFICATE AND PRIVATE KEY-----";

    protected PasswordCryptoService service;

    @Before
    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        this.service = getComponentManager().getInstance(PasswordCryptoService.class);
    }

    @Test
    public void testExportImport() throws Exception
    {
        // A trick to save CPU during test, see: X509KeyServiceTest#getKeyPair
        final XWikiX509KeyPair keyPair = X509KeyServiceTest.getKeyPair(this.service);

        final String exported = keyPair.serializeAsBase64();
        final XWikiX509KeyPair imported = DefaultXWikiX509KeyPair.fromBase64String(exported);

        Assert.assertEquals(keyPair.serializeAsBase64(), imported.serializeAsBase64());
        Assert.assertEquals(keyPair.getPrivateKey(this.password), imported.getPrivateKey(this.password));
        Assert.assertEquals(keyPair.getCertificate(), imported.getCertificate());
        Assert.assertEquals(keyPair, imported);
    }

    @Test
    public void importRegressionTest() throws Exception
    {
        XWikiX509KeyPair imported = DefaultXWikiX509KeyPair.fromBase64String(keyPairExportedAsBase64);
        Assert.assertNotNull(imported.getPrivateKey(this.password));
    }

    @Test
    public void testUID() throws GeneralSecurityException, IOException, ClassNotFoundException
    {
        XWikiX509KeyPair imported = DefaultXWikiX509KeyPair.fromBase64String(keyPairExportedAsBase64);
        XWikiX509Certificate cert = imported.getCertificate();
        Assert.assertEquals("xwiki:XWiki.Me", cert.getAuthorUID());
    }
}
