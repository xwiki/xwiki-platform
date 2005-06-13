/**
 * ===================================================================
 *
 * Copyright (c) 2003-2005 XpertNet, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details, published at
 * http://www.gnu.org/copyleft/gpl.html or in gpl.txt in the
 * root folder of this distribution.
 *
 */

package com.xpn.xwiki.web.includeservletasstring;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.ServletOutputStream;

/**
 *
 * @author LBlaze
 */
public class BufferOutputStream
extends ServletOutputStream {
  
    protected ByteArrayOutputStream buffer;
  
    /** Creates a new instance of BufferOutputStream */
    public BufferOutputStream() {
      
        buffer = new ByteArrayOutputStream();
      
    }
    
    
    public void write(int b)
    throws IOException
    {
        buffer.write(b);
    }
    
    public void write(byte b[])
    throws IOException
    {
        buffer.write(b);
    }
    
    public void write(byte[] b, int off, int len)
    throws IOException
    {
        buffer.write(b, off, len);
    }
    
    
    public void flush()
    throws IOException
    {
        buffer.flush();
    }
    
    public void close()
    throws IOException
    {
        buffer.close();
    }
    
  
    public byte[] getContentsAsByteArray()
    throws IOException
    {
      flush();
      return buffer.toByteArray();
    }
    
}
