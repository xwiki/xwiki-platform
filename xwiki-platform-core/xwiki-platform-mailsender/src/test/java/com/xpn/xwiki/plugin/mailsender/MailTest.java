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
package com.xpn.xwiki.plugin.mailsender;

import junit.framework.TestCase;

/**
 * Unit tests for {@link com.xpn.xwiki.plugin.mailsender.Mail}.
 */
public class MailTest extends TestCase
{
    public void testToString()
    {
        Mail mail = new Mail();
        mail.setFrom("john@acme.org");
        mail.setTo("peter@acme.org");
        mail.setSubject("Test subject");
        mail.setTextPart("Text content");
        mail.setHeader("header1", "value1");
        mail.setHeader("header2", "value2");
        assertEquals("From [john@acme.org], To [peter@acme.org], Subject [Test subject], Text [Text content], "
            + "Headers [[header1] = [value1][header2] = [value2]]", mail.toString());
    }
}
