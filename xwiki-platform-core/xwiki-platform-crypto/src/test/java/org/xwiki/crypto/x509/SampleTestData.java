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

/**
 * Some keys and certificate used for testing.
 *
 * @version $Id$
 */
public class SampleTestData
{
    /** No instance of this class needs to be created. */
    public SampleTestData() {}

    /** Fingerprint of the test certificate (XWiki SAS Web Certificate). */
    public static final String CERT_SHA1 = "eb31104d2fb1bc8495cf39e75124aef3f9ab7bfb";

    /** PEM encoded test certificate (XWiki SAS Web Certificate). */
    public static final String CERT_PEM = "-----BEGIN CERTIFICATE-----\n"
        + "MIIDWTCCAsKgAwIBAgIDEl9SMA0GCSqGSIb3DQEBBQUAME4xCzAJBgNVBAYTAlVT\n"
        + "MRAwDgYDVQQKEwdFcXVpZmF4MS0wKwYDVQQLEyRFcXVpZmF4IFNlY3VyZSBDZXJ0\n"
        + "aWZpY2F0ZSBBdXRob3JpdHkwHhcNMTAwNDE2MDI0NTU3WhcNMTEwNTE5MDEzNjIw\n"
        + "WjCB4zEpMCcGA1UEBRMgQnZ2MGF3azJ0VUhSOVBCdG9VdndLbEVEYVBpbkpoanEx\n"
        + "CzAJBgNVBAYTAkZSMRcwFQYDVQQKFA4qLnh3aWtpc2FzLmNvbTETMBEGA1UECxMK\n"
        + "R1Q0MDc0ODAzNjExMC8GA1UECxMoU2VlIHd3dy5yYXBpZHNzbC5jb20vcmVzb3Vy\n"
        + "Y2VzL2NwcyAoYykxMDEvMC0GA1UECxMmRG9tYWluIENvbnRyb2wgVmFsaWRhdGVk\n"
        + "IC0gUmFwaWRTU0woUikxFzAVBgNVBAMUDioueHdpa2lzYXMuY29tMIGfMA0GCSqG\n"
        + "SIb3DQEBAQUAA4GNADCBiQKBgQCSiflt/i6ZlqNODL8LQLPwNfXEdb3J+II1NXye\n"
        + "InrU3yRCybF7DG8NGIrvy+0o40YI+I4Q1Fcvv890IObdQdHmFtz8OKzKXT+giEG7\n"
        + "LxJXW3DDb9NckOsbjbNuNFSA9E/aQalrxbDVWyO0droG1v3vDBmG/KzfQkPmoE8g\n"
        + "P4qPsQIDAQABo4GuMIGrMB8GA1UdIwQYMBaAFEjmaPkr0rKV10fYIyAQTzOYkJ/U\n"
        + "MA4GA1UdDwEB/wQEAwIE8DAdBgNVHSUEFjAUBggrBgEFBQcDAQYIKwYBBQUHAwIw\n"
        + "OgYDVR0fBDMwMTAvoC2gK4YpaHR0cDovL2NybC5nZW90cnVzdC5jb20vY3Jscy9z\n"
        + "ZWN1cmVjYS5jcmwwHQYDVR0OBBYEFHbS5h/MPHDXIIn5ived2HiF6AwiMA0GCSqG\n"
        + "SIb3DQEBBQUAA4GBALPfA0VQS9pCFYl9co6k3AYLx+gWg6FsTn3aYZRjS9Eeg2qR\n"
        + "f7XuiIlq2ZLb1r0SA8Unn2uw2wrHXnqw2I/AARawI/vT4toKGjJwLB8cONLE6cyO\n"
        + "rC4qW/5AUann6D1r26EWLSGYh62AcX/jUT4bjoWLhMhblxyLOgbBe8uYPLMH\n"
        + "-----END CERTIFICATE-----\n";

    /** Expected author name in the test certificate (XWiki SAS Web Certificate). */
    public static final String CERT_AUTHOR = "CN=*.xwikisas.com, OU=Domain Control Validated - RapidSSL(R), "
        + "OU=See www.rapidssl.com/resources/cps (c)10, OU=GT40748036, "
        + "O=*.xwikisas.com, C=FR, SERIALNUMBER=Bvv0awk2tUHR9PBtoUvwKlEDaPinJhjq";

    /** Expected issuer name in the test certificate (XWiki SAS Web Certificate). */
    public static final String CERT_ISSUER = "OU=Equifax Secure Certificate Authority, O=Equifax, C=US";

    /** A key pair and certificate serialized */
    public static final String KEY_PAIR_EXPORTED_AS_BASE_64 = "-----BEGIN XWIKI CERTIFICATE AND PRIVATE KEY-----\n"
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

    /** The fingerprint of the certificate from the above KP and certificate serialization */
    public static final String CERTIFICATE_FINGERPRINT = "15c1fc76cf666afde744c74495d8a9972eaffb7d";

    /** Some text signed using the FF crypto.signText() API */
    public static final String BROWSER_SIGNED_TEXT = "This text was signed in Firefox 3.6.6-Linux using crypto.signText()";

    /** Resulting signature from the above text */
    public static final String BROWSER_SIGNATURE =
          "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\n"
        + "3ubN40e3RK5UCA==";

    /** Browser certificate used for signing */
    public static final String BROWSER_CERT = "-----BEGIN CERTIFICATE-----\n"
        + "MIIECTCCAvGgAwIBAgIQctJ7LkJyJ5tpIxaReDf4ZjANBgkqhkiG9w0BAQUFADCB\n"
        + "hTERMA8GA1UECgwIRk9BRitTU0wxJjAkBgNVBAsMHVRoZSBDb21tdW5pdHkgT2Yg\n"
        + "U2VsZiBTaWduZXJzMUgwRgYKCZImiZPyLGQBAQw4aHR0cDovLzEyNy4wLjAuMTo4\n"
        + "MDgxL3h3aWtpVHJ1bmsvYmluL3ZpZXcvWFdpa2kvQWRtaW4jbWUwHhcNMTAwNzA2\n"
        + "MDIxOTMxWhcNMTEwNjI3MDMxOTMxWjCBhTERMA8GA1UECgwIRk9BRitTU0wxJjAk\n"
        + "BgNVBAsMHVRoZSBDb21tdW5pdHkgT2YgU2VsZiBTaWduZXJzMUgwRgYKCZImiZPy\n"
        + "LGQBAQw4aHR0cDovLzEyNy4wLjAuMTo4MDgxL3h3aWtpVHJ1bmsvYmluL3ZpZXcv\n"
        + "WFdpa2kvQWRtaW4jbWUwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCW\n"
        + "WpeXr+SS8h0U4mxsK/Q8v/sDx6/yiNpPcUemn9aGXoCfYktq+fQsPNhu++vKo04K\n"
        + "jgASJMZMGnrVLvMHGSYipCZpbT1hAwrZxB+Y9iVj2+RAhV7VF3K0LqPe/Cz10V92\n"
        + "LI+rlDwlIqhZqQL2zAg7ZsSJTcuIA69tCYE5uhSr3Sy4zAudC4jTOjDlNk3j4gti\n"
        + "mvsh2gyVK3i3YNDfESDfDoczneMbUDzLwxRuYfUxXKyspfuYbFU319e1aF5dPdaa\n"
        + "zFEp5D9NyYr8FAXfOp3lONhsQ1K5D5u5MwiPam93vgSOIWFnszq1M27i1Mx5Czfk\n"
        + "08dMLswJyzoHIOSpULvHAgMBAAGjczBxMAwGA1UdEwEB/wQCMAAwEQYJYIZIAYb4\n"
        + "QgEBBAQDAgWgMA4GA1UdDwEB/wQEAwIC7DAfBgNVHSMEGDAWgBS+AuoCdGnHO6bN\n"
        + "oIHPE5I6gv1P6TAdBgNVHQ4EFgQU75JEUpyacJwhMa/CSD8OHhUjfVEwDQYJKoZI\n"
        + "hvcNAQEFBQADggEBAMRiUVHmNCdXIMnymNi6Dno3lM7tFkC591A3n+GXe9t9QMxg\n"
        + "kSm5Zws24JLxIJeJOQtHH3txT0nCgFLNKEfYPYS2Tlcg8psWr4VCyLhrpn4aIwNe\n"
        + "/+0fSkqOzVRQSz7V9oNRfu9KHteAmqRZGdDcrzfeRPNkw5UVeyDRrjmTqxDAYgic\n"
        + "vEHv5nJ2mg9r6Pb54KtQRbeamureWVBAL47+KvY3R8XBh41IArd138h/+tQBO8WV\n"
        + "aat8EoWYskcsBl5EcG1S8CYCFxlDzDjlPREQKyrxzyd33Ph3tqkHCGq1YM7P/WIs\n"
        + "kopEgj+eNtI57zf3Z1hGCwS8TxRRctSzxBvbnUs=\n"
        + "-----END CERTIFICATE-----";

    /** This is a public key generated in the browser and passed to the server. */
    public static final String SPKAC_SERIALIZATION =
          "MIICTTCCATUwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDC4aLkoIHw\n"
        + "Lmpwkml3LQV6s502xXDOwHnTRrFoY22oZGveVXSB13StWsphCdpT9dDdx9nT6SmF\n"
        + "2bRdm3XjrcvmSDWdrRBlWc6lC1kDJw9DaGzmT14tWdfbuTzILO+Cp222Rpkzpq+m\n"
        + "OkBmWcoU8yZpKgYkandRBrnC68gkQCXp1eTV7EIbcKhkrlv0n7TQj7zL9aTN9ZnH\n"
        + "OE609oTAN7o1qJsa3yOMKzwITrHXmQhLaEBysSEZP8b2LEvpv0KkPYaYVYSIPHM0\n"
        + "SdPx9j87tC1R815Hgn5//LNYo4avG0uS+hgVtXwd1qi5efCmz6+B6if87BUJpBhk\n"
        + "Ix+4/3+3z6MtAgMBAAEWDVRoZUNoYWxsZW5nZTEwDQYJKoZIhvcNAQEEBQADggEB\n"
        + "AH7+VePd7la+7sL1NtvaLsKbQFIiOOaxBr4DTMtZx1BrhMIdxfoBN/GBMPiqUDfX\n"
        + "eTv83Mv5KjGZNiDVjiXhbxWynv2zmLbLF/bcm8HOSnkRm0R24+bEHQqw1uDN1kj6\n"
        + "CLf7YFNlEQI8HpAzala/zuyZNuIACHwD8i5lc2+2SnhDs7ric+BCVTi/wfzi+Od2\n"
        + "0jdrXNw75ycww8oLFw46kuwywUl2Z1V8qZ5QOF6GIUClOSrwAlgxhstItSu8+sur\n"
        + "oFfUOwW0YIX4zJEqD+aB5hxN8hP+wJG/mYYoVIZFRkk9t4l2ZEo5Avu5aTfEuDUX\n"
        + "yj8rDgFwLxTXBpBxvdiRAgw=";

    public static final String PASSWORD = "blah";
}
