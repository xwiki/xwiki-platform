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
package org.xwiki.crypto.internal;

import java.io.ByteArrayOutputStream;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class LineWrapperOutputStreamTest
{
    /** New line bytes used to wrap lines */
    private static final String NEWLINE = System.getProperty("line.separator", "\n");

    @Test
    public void testAvoidWrapping() throws Exception
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        LineWrapperOutputStream lwos = new LineWrapperOutputStream(baos, 10);
        lwos.write("12345".getBytes());
        lwos.write("1234567890".getBytes(),5,4);
        lwos.close();
        assertThat(baos.toString(), equalTo("123456789"));
    }

    @Test
    public void testExactWrapping() throws Exception
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        LineWrapperOutputStream lwos = new LineWrapperOutputStream(baos, 10);
        lwos.write("789012345".getBytes(),4,5);
        lwos.write("67890".getBytes());
        lwos.close();
        assertThat(baos.toString(), equalTo("1234567890" + NEWLINE));
    }

    @Test
    public void testMultilineWrapping() throws Exception
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        LineWrapperOutputStream lwos = new LineWrapperOutputStream(baos, 10);
        lwos.write("12345".getBytes());
        lwos.write("3456789012345678".getBytes(),3,10);
        lwos.write("6789012345".getBytes(),0,7);
        lwos.close();
        assertThat(baos.toString(), equalTo("1234567890" + NEWLINE + "1234567890" + NEWLINE + "12"));
    }

    @Test
    public void testOnByteWrapping() throws Exception
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        LineWrapperOutputStream lwos = new LineWrapperOutputStream(baos, 10);
        lwos.write("123456789".getBytes());
        lwos.write("0".getBytes()[0]);
        lwos.write("12345".getBytes());
        lwos.close();
        assertThat(baos.toString(), equalTo("1234567890" + NEWLINE + "12345"));
    }
}
