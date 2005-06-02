/**
 * ===================================================================
 *
 * Copyright (c) 2005 Jérémi Joslin, All rights reserved.
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
 * User: jeremi
 * Date: May 10, 2005
 * Time: 8:51:00 AM
 */
package com.xpn.xwiki.plugin.packaging;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.XWiki;

import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.io.StringReader;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.DocumentException;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.dom4j.io.SAXReader;
import org.dom4j.dom.DOMDocument;
import org.dom4j.dom.DOMElement;



public class Package {
    private String  name = "My package";
    private String  description = "";
    private String  version = "1.0.0";
    private String  licence = "GPL";
    private String  authorName = "XWiki";
    private String  spaceName = null;
    private List    files = null;
    private boolean upgradePossible = false;
    private boolean backupPack = false;
    private boolean withVersions = true;

    public static final int OK = 0;
    public static final int Right = 1;
    public static final String DefaultPackageFileName = "package.xml";

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getLicence() {
        return licence;
    }

    public void setLicence(String licence) {
        this.licence = licence;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public boolean isBackupPack() {
        return backupPack;
    }

    public void setBackupPack(boolean backupPack) {
        this.backupPack = backupPack;
    }

    public String getSpaceName() {
        return spaceName;
    }

    public boolean setSpaceName(String spaceName) {
        if (spaceName.compareTo("XWiki") != 0)
            return false;
        this.spaceName = spaceName;
        for (int i = 0; i < files.size(); i++)
            ((DocumentInfo)files.get(i)).setDefaultSpace(spaceName);
        return true;
    }

    public List getFiles() {
        return files;
    }

    public boolean getUpgradePossible() {
        return upgradePossible;
    }

    public void setUpgradePossible(boolean upgradePossible) {
        this.upgradePossible = upgradePossible;
    }


    public boolean isInstalled() {
        if (files.size() == 0)
            return (false);
        for (int i = 0; i < files.size(); i++)
        {
            if (((DocumentInfo)files.get(i)).isNew())
                return false ;
        }
        return true;
    }

    public boolean isWithVersions() {
        return withVersions;
    }

    public void setWithVersions(boolean withVersions) {
        this.withVersions = withVersions;
    }

    public Package()
    {
        files = new ArrayList();

    }

    public boolean add(XWikiDocument doc, int defaultAction, XWikiContext context) throws XWikiException {
//        if ((doc.getWeb().compareTo("XWiki") != 0) && (spaceName != null) && (doc.getWeb().compareTo(spaceName) != 0))
//            return false;
        if (!context.getWiki().checkAccess("edit", doc, context))
            return false;
//        if (spaceName == null && (doc.getWeb().compareTo("XWiki") != 0))
//            spaceName = doc.getWeb();
        for(int i = 0; i < files.size(); i++)
        {
            if (((DocumentInfo)files.get(i)).getFullName().compareTo(doc.getFullName()) == 0)
            {
                if (defaultAction != DocumentInfo.ACTION_NOT_DEFINED)
                    ((DocumentInfo)files.get(i)).setAction(defaultAction);
                if (!doc.isNew())
                    ((DocumentInfo)files.get(i)).setDoc(doc);
                return true;
            }
        }
        DocumentInfo docinfo = new DocumentInfo(doc);
        docinfo.setAction(defaultAction);
        files.add(docinfo);
        return true;
    }

    public boolean add(XWikiDocument doc, XWikiContext context) throws XWikiException {
        return add(doc, DocumentInfo.ACTION_NOT_DEFINED, context);
    }

    public boolean updateDoc(String docFullName, int action, XWikiContext context) throws XWikiException {
        XWikiDocument doc = new XWikiDocument();
        doc.setFullName(docFullName, context);
        return add(doc, action, context);           
    }

    public boolean add(String docFullName, int DefaultAction, XWikiContext context) throws XWikiException {
        XWikiDocument doc = context.getWiki().getDocument(docFullName, context);
        if (doc.isNew())
            return (false);
        add(doc, DefaultAction, context);
        return true;
    }


    public boolean add(String docFullName, XWikiContext context) throws XWikiException {
        return add(docFullName, DocumentInfo.ACTION_NOT_DEFINED, context);
    }

    public String Export(XWikiContext context) throws IOException, XWikiException {
        if (files.size() == 0)
        {
            return "No Selected file";
        }

        ZipOutputStream zos = new ZipOutputStream(context.getResponse().getOutputStream());
        context.setFinished(true);
        for (int i = 0; i < files.size(); i++)
        {
            DocumentInfo docinfo = (DocumentInfo) files.get(i);
            docinfo.getDoc().addToZip(zos, withVersions, context);
        }
        addToZip(zos);
        zos.finish();
        zos.flush();
        return "";
    }

    public String Import(byte file[], XWikiContext context) throws IOException, XWikiException, DocumentException {
        ByteArrayInputStream    bais = new ByteArrayInputStream(file);
        ZipInputStream          zis = new ZipInputStream(bais);
        ZipEntry                entry;
        Document                description = null;
        ArrayList               docs = new ArrayList();

        description = ReadZipInfoFile(zis);
        bais = new ByteArrayInputStream(file);
        zis = new ZipInputStream(bais);
        while ((entry = zis.getNextEntry()) != null)
        {
            if(entry.getName().compareTo(DefaultPackageFileName) == 0)
                continue;
            else
            {
                XWikiDocument doc = readZipDoc(readZipFile(zis));

                this.add(doc, context);
            }
        }


        if (description == null)
                files.clear();
            else
                updateFileInfos(description);
        return "";
    }

    private void updateFileInfos(Document xml)
    {
        Element docFiles = xml.getRootElement();
        Element infosFiles = docFiles.element("files");

        List ListFile =  infosFiles.elements("file");
        for (int i = 0; i < ListFile.size(); i++)
        {
            String defaultAction = ((Element)ListFile.get(i)).attributeValue("defaultAction");
            String docName = ((Element)ListFile.get(i)).getStringValue();
            setDocumentDefaultAction(docName, Integer.parseInt(defaultAction));
        }
    }

    private void setDocumentDefaultAction(String docName, int defaultAction)
    {
        if (files == null)
            return;
        for(int i = 0; i < files.size(); i++)
        {
            if (((DocumentInfo)files.get(i)).getFullName().compareTo(docName) == 0)
            {
                ((DocumentInfo)files.get(i)).setAction(defaultAction);
                return;
            }
        }
    }

    public int TestInstall(XWikiContext context)
    {
        int result = DocumentInfo.INSTALL_IMPOSSIBLE;
        if (files.size() == 0)
            return result;

        result = ((DocumentInfo)files.get(0)).testInstall(context);
        for (int i = 1; i < files.size(); i++)
        {
            int res = ((DocumentInfo)files.get(i)).testInstall(context);
            if (res < result)
                result = res;
        }
        return result;
    }

    public int install(XWikiContext context)
    {
        if (TestInstall(context) == DocumentInfo.INSTALL_IMPOSSIBLE)
            return DocumentInfo.INSTALL_IMPOSSIBLE;

        for (int i = 0; i < files.size(); i++)
        {
            int res = ((DocumentInfo)files.get(i)).install(context);
        }
        return DocumentInfo.INSTALL_OK;
    }


    private String readZipFile(ZipInputStream zis) throws IOException{
        byte[] data = new byte[4096];
        StringBuffer XmlFile = new StringBuffer();
        int Cnt;
        while ((Cnt = zis.read(data, 0, 4096)) != -1) {
          XmlFile.append(new String(data, 0, Cnt)) ;
        }
        return XmlFile.toString();
    }

    private XWikiDocument readZipDoc(String XmlFile) throws IOException{
        XWikiDocument doc = new com.xpn.xwiki.doc.XWikiDocument();
        try {
            if (backupPack && withVersions)
                doc.fromXML(XmlFile.toString(), true);
            else
                doc.fromXML(XmlFile.toString());
        } catch (Exception e) {
            return null;
        }
        return doc;
    }

    private Document ReadZipInfoFile(ZipInputStream zis) throws IOException, DocumentException {
        ZipEntry    entry;
        Document    description;

        while ((entry = zis.getNextEntry()) != null)
        {
            if(entry.getName().compareTo(DefaultPackageFileName) == 0)
            {
                description = ReadZipPackage(zis);
                return description;
            }
        }
        return null;
    }

    private Document ReadZipPackage(ZipInputStream zis) throws IOException, DocumentException{
        byte[] data = new byte[4096];
        StringBuffer XmlFile = new StringBuffer();
        int Cnt;
        while ((Cnt = zis.read(data, 0, 4096)) != -1) {
          XmlFile.append(new String(data, 0, Cnt)) ;
        }
        return fromXml(XmlFile.toString());
    }

    public String toXml()
    {
       OutputFormat outputFormat = new OutputFormat("", true);
        outputFormat.setEncoding("UTF-8");
        StringWriter out = new StringWriter();
        XMLWriter writer = new XMLWriter( out, outputFormat );
        try {
            writer.write(toXmlDocument());
            return out.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    public Document toXmlDocument()
    {
        Document doc = new DOMDocument();
        Element docel = new DOMElement("package");
        doc.setRootElement(docel);

        Element elInfos = new DOMElement("infos");
        docel.add(elInfos);

        Element el = new DOMElement("name");
        el.addText(name);
        elInfos.add(el);

        el = new DOMElement("description");
        el.addText(description);
        elInfos.add(el);

        el = new DOMElement("licence");
        el.addText(licence);
        elInfos.add(el);

        el = new DOMElement("author");
        el.addText(authorName);
        elInfos.add(el);

        el = new DOMElement("version");
        el.addText(version);
        elInfos.add(el);

        el = new DOMElement("backupPack");
        el.addText(new Boolean(backupPack).toString());
        elInfos.add(el);


        Element elfiles = new DOMElement("files");
        docel.add(elfiles);

        for (int i = 0; i < files.size(); i++)
        {
            Element elfile = new DOMElement("file");
            elfile.addAttribute("defaultAction", String.valueOf(((DocumentInfo)(files.get(i))).getAction()));
            elfile.addText(((DocumentInfo)(files.get(i))).getFullName());
            elfiles.add(elfile);
        }
        return doc;
    }

    public void addToZip(ZipOutputStream zos) throws IOException {
        try  {
        String zipname = DefaultPackageFileName;
        ZipEntry zipentry = new ZipEntry(zipname);
        zos.putNextEntry(zipentry);
        zos.write(toXml().getBytes());
        zos.closeEntry();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

     protected String getElementText(Element docel, String name) {
         Element el = docel.element(name);
         if (el==null)
             return "";
         else
             return el.getText();
     }

    protected Document fromXml(String xml) throws DocumentException {
        SAXReader reader = new SAXReader();
        Document domdoc;

        StringReader in = new StringReader(xml);
        domdoc = reader.read(in);

        Element docEl = domdoc.getRootElement();
        Element infosEl = docEl.element("infos");

        name = getElementText(infosEl, "name");
        description = getElementText(infosEl, "description");
        licence  = getElementText(infosEl, "licence");
        authorName = getElementText(infosEl, "author");
        version = getElementText(infosEl, "version");
        backupPack = new Boolean(getElementText(infosEl, "backupPack")).booleanValue();
        return domdoc;
    }


    public void backupWiki(XWikiContext context) throws XWikiException {
        XWiki wiki = context.getWiki();
        List spaces = wiki.getSpaces(context);
        name = "Backup";
        description = "on " + (new Date().toString()) + " by " + context.getUser();
        for(int i = 0; i < spaces.size(); i++)
        {
            List DocsName = wiki.getSpaceDocsName((String) spaces.get(i), context);
            for(int j = 0; j < DocsName.size(); j++)
                this.add(spaces.get(i) + "." + DocsName.get(j), DocumentInfo.ACTION_OVERWRITE,  context);
        }
        this.backupPack = true;
    }


    public static Package OpenInformations(String PackageName)
    {
        return null;
    }

    public static List getPackageList()
    {

        return null;
    }


}

