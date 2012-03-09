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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.security.KeyPair;

import org.xwiki.crypto.internal.Convert;

/**
 * This is a support class for tests which supplies KeyPairs very fast.
 * It should be obvious to the reader that this is not secure since it always provides the same key
 * and the private key is written in the string below.
 *
 * @version $Id$
 * @since 3.1M2
 */
public class FastKeySupplier
{
    private static final byte[] SERIALIZED_KEYPAIR = Convert.fromBase64String(
        "rO0ABXNyABVqYXZhLnNlY3VyaXR5LktleVBhaXKXAww60s0SkwIAAkwACnByaXZhdGVLZXl0ABpMamF2YS9zZWN1cm"
      + "l0eS9Qcml2YXRlS2V5O0wACXB1YmxpY0tleXQAGUxqYXZhL3NlY3VyaXR5L1B1YmxpY0tleTt4cHNyADFvcmcuYm91"
      + "bmN5Y2FzdGxlLmpjZS5wcm92aWRlci5KQ0VSU0FQcml2YXRlQ3J0S2V5bLqHzgJzVS4CAAZMAA5jcnRDb2VmZmljaW"
      + "VudHQAFkxqYXZhL21hdGgvQmlnSW50ZWdlcjtMAA5wcmltZUV4cG9uZW50UHEAfgAFTAAOcHJpbWVFeHBvbmVudFFx"
      + "AH4ABUwABnByaW1lUHEAfgAFTAAGcHJpbWVRcQB+AAVMAA5wdWJsaWNFeHBvbmVudHEAfgAFeHIALm9yZy5ib3VuY3"
      + "ljYXN0bGUuamNlLnByb3ZpZGVyLkpDRVJTQVByaXZhdGVLZXlG6wnAB89BHAMAA0wAC2F0dHJDYXJyaWVydAA9TG9y"
      + "Zy9ib3VuY3ljYXN0bGUvamNlL3Byb3ZpZGVyL1BLQ1MxMkJhZ0F0dHJpYnV0ZUNhcnJpZXJJbXBsO0wAB21vZHVsdX"
      + "NxAH4ABUwAD3ByaXZhdGVFeHBvbmVudHEAfgAFeHBzcgAUamF2YS5tYXRoLkJpZ0ludGVnZXKM/J8fqTv7HQMABkkA"
      + "CGJpdENvdW50SQAJYml0TGVuZ3RoSQATZmlyc3ROb256ZXJvQnl0ZU51bUkADGxvd2VzdFNldEJpdEkABnNpZ251bV"
      + "sACW1hZ25pdHVkZXQAAltCeHIAEGphdmEubGFuZy5OdW1iZXKGrJUdC5TgiwIAAHhw///////////////+/////gAA"
      + "AAF1cgACW0Ks8xf4BghU4AIAAHhwAAABAKjl2xqUACSbT5H/Hyi9VysuCoxOQQ0nj94F5Bj/g8VlU+giVG6DiJ918s"
      + "WdgTme42vM3KaXoZD3gEJaMWFI1kWFDqm3KPXHDRXIcXEDpdx04jmUdog9S8sg9bsguH3vZyONGkfkIJHqENv+yNoy"
      + "Uq0OAOG/5F3HybkmYVJRvstPagmK8ZhYnYv8iZrOxPoaPPOqwEEU/6HUmMLUsVElA8FL33R7y9VP4Jhc6hP3PU/ANZ"
      + "KnW+j13bIJnjhKfOe5Z/D6/nXrXx1a0UQ0NqqpJGXead8eeI8Htk7FmhEak2Y2E85uiqn+zAfkURKdzAnpzl1knibT"
      + "Z7AcpxXIeAprP2d4c3IAE2phdmEudXRpbC5IYXNodGFibGUTuw8lIUrkuAMAAkYACmxvYWRGYWN0b3JJAAl0aHJlc2"
      + "hvbGR4cD9AAAAAAAAIdwgAAAALAAAAAHhzcgAQamF2YS51dGlsLlZlY3RvctmXfVuAO68BAwADSQARY2FwYWNpdHlJ"
      + "bmNyZW1lbnRJAAxlbGVtZW50Q291bnRbAAtlbGVtZW50RGF0YXQAE1tMamF2YS9sYW5nL09iamVjdDt4cAAAAAAAAA"
      + "AAdXIAE1tMamF2YS5sYW5nLk9iamVjdDuQzlifEHMpbAIAAHhwAAAACnBwcHBwcHBwcHB4c3EAfgAJ////////////"
      + "///+/////gAAAAF1cQB+AA0AAAEAhVikegFrc3tjgSLpWOLUFXVOQqPt3BLmFSaVhd8GDC0J5/ZirrYOgcc/myHVSs"
      + "bVSuD9PFHu72NY0goffm1hLpX0k63WGuXCwPX2CoIUy09E5K0AQGQUmRU2rT0SK+t9hKX8n9HOnSUtlmM79HlSqOqr"
      + "a5BzHk+ERje1EqcTebO0lIpLBF/aSjWOzC5XHtWWAjn/G41b+JWBYd58NFPDA67sA7n0LSYfcEiUqSVoIvDjfFpMOZ"
      + "uhkaWnlUZTyjtE+iLTXwvuLNcwObu2jvUJNWXGToJ5F/sF2P5uMZS8eAwdevKC0rKCXYe3St1u/M9f/rJUxLHUdQ/5"
      + "sJeWoaTAUXh4c3EAfgAJ///////////////+/////gAAAAF1cQB+AA0AAACA7FnV33tnbZQGvLY7sPv8Em8g2zTm7z"
      + "b8mit+wNlkxxl2x8c6NUL+DJArdevlPu0Td2YHkDMp2dnyiFz4CA1NSueVzsvAMl5roqre65YW97qk6pjt8FOgLqpl"
      + "Lq6epz/3bOTkw92rSdhpXSC9HGRcToFJQQI3LM0kcScIgCJUQa94c3EAfgAJ///////////////+/////gAAAAF1cQ"
      + "B+AA0AAACA83qM7px4oe39a5O+0iRAo1Hn5WyuKhlDXYsKelZU7DBZY9rg2VucQDVhpBRIS7YrZ2InarYUnEu+PMrr"
      + "kvQw1vRMhs/IjMWb4O92kedjq26pj9kqN6GIQMhvZ7DKRpv1BxykbsmB9Or4SMZ1hZaENk1SlLlItIuhaXM0i3Epnd"
      + "V4c3EAfgAJ///////////////+/////gAAAAF1cQB+AA0AAACAb2+QekcY9BDn9NH1uIJ+T1YG5G/Cm9hCWtLgU4Yg"
      + "+S8vqr7kBTTlesJIbTHxlnbXvOQERFrPZtSCMpji0I/9wU+QMo119oBCGD/wnRjZKjC2Uru4lMxQJQgn1lJCx3cksU"
      + "RkQc8yBwPP+k4l0tHJfkphh/EupEt0AYgl+m6VvPF4c3EAfgAJ///////////////+/////gAAAAF1cQB+AA0AAACA"
      + "/RAIzoLIubZwcUsjmem0t/2srdVDaRTQLEuxmZIgbtd77DBEe/GmitlFhCvZVUG5EBJQSPBg7cCBLGiJejlI90m0zE"
      + "w+Q/8kVa2vxdpvhaBcGyXTqRVoWqc/ChC+OwAG5evvE2kN3c+WAHlWOKcVD/EHzrYxH+qnEjxY16e1Y+14c3EAfgAJ"
      + "///////////////+/////gAAAAF1cQB+AA0AAACAqtu6rkHyprN1eC18HCBVlXeUpDC40phy6goIYA5/KEvBfCJk7f"
      + "16CGIlLW+VG7JGKs8WbPLxNGR65FMdI87UvgHs8XfhvhvXUyQ6PqGZxYJf8VnPIbZW06Jb42iNl/328mYvFn3wUlmL"
      + "LSu+2AnAkD3nGVzAWzD4E4aJhrYSLiN4c3EAfgAJ///////////////+/////gAAAAF1cQB+AA0AAAADAQABeHNyAC"
      + "1vcmcuYm91bmN5Y2FzdGxlLmpjZS5wcm92aWRlci5KQ0VSU0FQdWJsaWNLZXklImoOW/pshAIAAkwAB21vZHVsdXNx"
      + "AH4ABUwADnB1YmxpY0V4cG9uZW50cQB+AAV4cHEAfgAMcQB+ACI=");

    public static final KeyPair KEYPAIR;

    static
    {
        try {
            final InputStream is = new ByteArrayInputStream(SERIALIZED_KEYPAIR);
            final ObjectInputStream ois = new ObjectInputStream(is);
            KEYPAIR = (KeyPair) ois.readObject();
        } catch (Exception e) {
            throw new RuntimeException("Something went wrong deserializing the keypair.", e);
        }
    }
}
