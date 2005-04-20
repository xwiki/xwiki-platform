/**
 * ===================================================================
 *
 * Copyright (c) 2003,2004 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details, published at 
 * http://www.gnu.org/copyleft/lesser.html or in lesser.txt in the
 * root folder of this distribution.

 * Created by
 * User: Ludovic Dubost
 * Date: 16 août 2004
 * Time: 10:56:46
 */
package com.xpn.xwiki.pdf.api;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

import java.io.OutputStream;

public interface PdfExport {
    public void exportXHtml(byte[] xhtml, OutputStream out, int type) throws XWikiException;
    public void exportHtml(String xhtml, OutputStream out, int type) throws XWikiException;
    public void export(XWikiDocument doc, OutputStream out, int type, XWikiContext context) throws XWikiException;
    public void exportToPDF(XWikiDocument doc, OutputStream out, XWikiContext context) throws XWikiException;
    public byte[] convertToStrictXHtml(byte[] input);
    public byte[] convertXHtmlToXMLFO(byte[] input) throws XWikiException;
}
