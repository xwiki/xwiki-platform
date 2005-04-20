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
 * Date: 19 août 2004
 * Time: 12:49:56
 */
package com.xpn.xwiki.test;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.pdf.impl.PdfExportImpl;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.XWikiCacheStoreInterface;
import com.opensymphony.module.access.provider.osuser.OSUserUserProvider;
import org.hibernate.HibernateException;
import junit.framework.TestCase;

import java.io.ByteArrayOutputStream;

public class PDFExportTest extends TestCase {

    private XWiki xwiki;
    private XWikiContext context;

    public static boolean inTest = false;

    public XWikiHibernateStore getHibStore() {
        XWikiStoreInterface store = xwiki.getStore();
        if (store instanceof XWikiCacheStoreInterface)
            return (XWikiHibernateStore)((XWikiCacheStoreInterface)store).getStore();
        else
            return (XWikiHibernateStore) store;
    }

    public void setUp() throws HibernateException, XWikiException {
        OSUserUserProvider toto = new OSUserUserProvider();
        context = new XWikiContext();
        context.setDatabase("xwikitest");
        xwiki = new XWiki("./xwiki.cfg", context);
        xwiki.setDatabase("xwikitest");
        context.setWiki(xwiki);
        StoreHibernateTest.cleanUp(getHibStore(), context);
        xwiki.flushCache();
    }

    public void tearDown() throws HibernateException {
        getHibStore().shutdownHibernate(context);
        xwiki = null;
        context = null;
        System.gc();
    }

    public String getHeader() {
       return "<?xml version=\"1.0\" encoding=\"iso-8859-1\" ?>\n" +
                "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"\n" +
                "    \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n" +
                "<html xmlns=\"http://www.w3.org/1999/xhtml\" lang=\"en\" xml:lang=\"en\">" +
                "<body>\n";
    }

    public String getFooter() {
        return "</body>\n</html>\n";
    }

    public void testXHTMLConversion() {
        PdfExportImpl pdfexport = new PdfExportImpl();
        pdfexport.setXhtmlxsl("xhtml2fo.xsl");
        String html = getHeader() + "<p>Hello" + getFooter();
        String xhtml = new String(pdfexport.convertToStrictXHtml(html.getBytes()));
        assertTrue("XHTML Conversion failed", (xhtml.indexOf("</p>")!=-1));
    }

    public void testPDFConversion() throws XWikiException {
        PdfExportImpl pdfexport = new PdfExportImpl();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String html = getHeader() + "<p>Hello</p>" + getFooter();
        pdfexport.exportHtml(html, out, 0);
        String result = new String(out.toByteArray());
        // assertTrue("PDF Conversion failed", (result.indexOf("Hello")!=-1));
    }

    public void testPDFConversionWithTable() throws XWikiException {
        PdfExportImpl pdfexport = new PdfExportImpl();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String html = getHeader() + "<table><tr><td>Hello</td><td>Ah</td></tr><tr><td>Hello</td><td>Ah</td><td>Hi</td></tr></table>" + getFooter();
        pdfexport.exportHtml(html, out, 0);
        String result = new String(out.toByteArray());
        //assertTrue("PDF Conversion failed", (result.indexOf("Hello")!=-1));
    }

    public void testPDFConversionWithEmptyTable() throws XWikiException {
        PdfExportImpl pdfexport = new PdfExportImpl();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String html = getHeader() + "<table></table>" + getFooter();
        pdfexport.exportHtml(html, out, 0);
        String result = new String(out.toByteArray());
        //assertTrue("PDF Conversion failed", (result.indexOf("Hello")!=-1));
    }

    public void testPDFConversionWithTableWithNoCols() throws XWikiException {
        PdfExportImpl pdfexport = new PdfExportImpl();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String html = getHeader() + "<table><tr></tr><tr></tr></table>" + getFooter();
        pdfexport.exportHtml(html, out, 0);
        String result = new String(out.toByteArray());
        //assertTrue("PDF Conversion failed", (result.indexOf("Hello")!=-1));
    }

    public void testPDFConversionWithImage() throws XWikiException {
        PdfExportImpl pdfexport = new PdfExportImpl();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String html = getHeader() + "<img src=\"toto.gif\" />" + getFooter();
        pdfexport.exportHtml(html, out, 0);
        String result = new String(out.toByteArray());
        //assertTrue("PDF Conversion failed", (result.indexOf("Hello")!=-1));
    }

}
