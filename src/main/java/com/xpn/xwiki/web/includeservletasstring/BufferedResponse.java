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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.OutputStreamWriter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 *
 * @author LBlaze
 */
public class BufferedResponse
extends HttpServletResponseWrapper {
  
    protected HttpServletResponse internalResponse;
    
    protected BufferOutputStream outputStream;
    protected PrintWriter writer;
  
    /** Creates a new instance of BufferedResponse */
    public BufferedResponse(HttpServletResponse internalResponse) {
      
        super(internalResponse);
      
        this.internalResponse = internalResponse;
    }

    
    public ServletOutputStream getOutputStream()
    throws IOException
    {
        if( outputStream == null )
        {
            outputStream = new BufferOutputStream();
        }
        return outputStream;
    }

    
    public PrintWriter getWriter()
    throws IOException
    {
        if( writer == null )
        {
            writer = new PrintWriter(new OutputStreamWriter(getOutputStream(), getCharacterEncoding()));
        }
        return writer;
    }
    
    
    public byte[] getBufferAsByteArray()
    throws IOException
    {
        if( writer != null )
        {
            writer.flush();
        }
        outputStream.flush();
        
        return outputStream.getContentsAsByteArray();
    }
    
  
}
