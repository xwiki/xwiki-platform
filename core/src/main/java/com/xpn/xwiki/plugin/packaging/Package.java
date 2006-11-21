/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
 *
 * @author jeremi
 */
package com.xpn.xwiki.plugin.packaging;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.dom.DOMDocument;
import org.dom4j.dom.DOMElement;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;


public class Package {
    private static final Log log = LogFactory.getLog(Package.class);

    private String  name = "My package";
    private String  description = "";
    private String  version = "1.0.0";
    private String  licence = "LGPL";
    private String  authorName = "XWiki";
    private List    files = null;
    private List    customMappingFiles = null;
    private boolean backupPack = false;
    private boolean withVersions = true;
    private List    documentFilters = new ArrayList();

    public static final int OK = 0;
    public static final int Right = 1;
    public static final String DefaultPackageFileName = "package.xml";
    public static final String DefaultPluginName = "package";

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

    public List getFiles() {
        return files;
    }

    public List getCustomMappingFiles() {
        return customMappingFiles;
    }

    public boolean isWithVersions() {
        return withVersions;
    }

    public void setWithVersions(boolean withVersions) {
        this.withVersions = withVersions;
    }

    public void addDocumentFilter(Object filter) throws PackageException {
        if (filter instanceof DocumentFilter)
            this.documentFilters.add(filter);
        else
            throw new PackageException(PackageException.ERROR_PACKAGE_INVALID_FILTER, "Invalid Document Filter");
    }

    public Package()
    {
        files = new ArrayList();
        customMappingFiles = new ArrayList();
    }

    public boolean add(XWikiDocument doc, int defaultAction, XWikiContext context) throws XWikiException {
        if (!context.getWiki().checkAccess("edit", doc, context))
            return false;
        for(int i = 0; i < files.size(); i++)
        {
            DocumentInfo di = (DocumentInfo)files.get(i);
            if (di.getFullName().equals(doc.getFullName())&&(di.getLanguage().equals(doc.getLanguage())))
            {
                if (defaultAction != DocumentInfo.ACTION_NOT_DEFINED)
                    di.setAction(defaultAction);
                if (!doc.isNew())
                    di.setDoc(doc);
                return true;
            }
        }

        doc = (XWikiDocument) doc.clone();

        try {
            filter(doc, context);

            DocumentInfo docinfo = new DocumentInfo(doc);
            docinfo.setAction(defaultAction);
            files.add(docinfo);
            BaseClass bclass =  doc.getxWikiClass();
            if (bclass.getCustomMapping()!=null)
                customMappingFiles.add(docinfo);
            return true;
        } catch (ExcludeDocumentException e) {
            log.info("Skip the document " + doc.getFullName());
            return false;
        }
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
        add(doc, DefaultAction, context);
        List languages = doc.getTranslationList(context);
        for (int i=0;i<languages.size();i++) {
            String language = (String) languages.get(i);
            if (!((language==null)||(language.equals(""))||(language.equals(doc.getDefaultLanguage()))))
                add(doc.getTranslatedDocument(language, context), DefaultAction, context);
        }
        return true;
    }

    public boolean add(String docFullName, String language, int DefaultAction, XWikiContext context) throws XWikiException {
        XWikiDocument doc = context.getWiki().getDocument(docFullName, context);
        if ((language==null)||(language.equals("")))
         add(doc, DefaultAction, context);
        else
         add(doc.getTranslatedDocument(language, context), DefaultAction, context);
        return true;
    }


    public boolean add(String docFullName, XWikiContext context) throws XWikiException {
        return add(docFullName, DocumentInfo.ACTION_NOT_DEFINED, context);
    }

    public boolean add(String docFullName, String language, XWikiContext context) throws XWikiException {
        return add(docFullName, language, DocumentInfo.ACTION_NOT_DEFINED, context);
    }

    public void filter(XWikiDocument doc, XWikiContext context) throws ExcludeDocumentException {
        for (int i = 0; i < documentFilters.size(); i++)
            ((DocumentFilter)documentFilters.get(i)).filter(doc, context);
    }

    public String export(OutputStream os, XWikiContext context) throws IOException, XWikiException {
        if (files.size() == 0)
        {
            return "No Selected file";
        }

        ZipOutputStream zos = new ZipOutputStream(os);
        for (int i = 0; i < files.size(); i++)
        {
            DocumentInfo docinfo = (DocumentInfo) files.get(i);
            XWikiDocument doc = docinfo.getDoc();
            addToZip(doc, zos, withVersions, context);
        }
        addInfosToZip(zos, context);
        zos.finish();
        zos.flush();
        return "";
    }

    public String exportToDir(File dir, XWikiContext context) throws IOException, XWikiException {
        if (!dir.exists()) {
           if (!dir.mkdirs()) {
               Object[] args = new Object[1];
               args[0] = dir.toString();
               throw new XWikiException(XWikiException.MODULE_XWIKI, XWikiException.ERROR_XWIKI_MKDIR, "Error creating directory {0}", null, args);
           }
        }

        for (int i = 0; i < files.size(); i++)
        {
            DocumentInfo docinfo = (DocumentInfo) files.get(i);
            XWikiDocument doc = docinfo.getDoc();
            addToDir(doc, dir, withVersions, context);
        }
        addInfosToDir(dir, context);
        return "";
    }


    public String Import(byte file[], XWikiContext context) throws IOException, XWikiException {
        ByteArrayInputStream    bais = new ByteArrayInputStream(file);
        ZipInputStream          zis = new ZipInputStream(bais);
        ZipEntry                entry;
        Document                description = null;
        ArrayList               docs = new ArrayList();

        try {
            description = ReadZipInfoFile(zis);
            if (description == null)
                throw new PackageException(PackageException.ERROR_XWIKI_UNKNOWN, "Could not find the package definition");
            bais = new ByteArrayInputStream(file);
            zis = new ZipInputStream(bais);
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().compareTo(DefaultPackageFileName) == 0 || entry.isDirectory())
                    continue;
                else {
                    XWikiDocument doc = readFromXML(readFromInputStream(zis));
                    try {
                        filter(doc, context);
                        if (documentExistInPackageFile(doc.getFullName(), doc.getLanguage(), description))
                            this.add(doc, context);
                        else
                            throw new PackageException(PackageException.ERROR_XWIKI_UNKNOWN, "document " + doc.getFullName() + " does not exist in package definition");
                    } catch (ExcludeDocumentException e) {
                        log.info("Skip the document '" + doc.getFullName() + "'");
                    }
                }
            }
            updateFileInfos(description);
        } catch (DocumentException e) {
            throw new PackageException(PackageException.ERROR_XWIKI_UNKNOWN, "Error when reading the XML");
        }
        return "";
    }

    private boolean documentExistInPackageFile(String docName, String language, Document xml)
    {
        Element docFiles = xml.getRootElement();
        Element infosFiles = docFiles.element("files");

        List ListFile =  infosFiles.elements("file");
        Iterator it = ListFile.iterator();
        while(it.hasNext())
        {
            Element el = (Element)it.next();
            String tmpDocName = el.getStringValue();
            if (tmpDocName.compareTo(docName) !=  0)
                continue;
            String tmpLanguage = el.attributeValue("language");
            if (tmpLanguage==null)
             tmpLanguage = "";
            if (tmpLanguage.compareTo(language) ==  0)
                return true;
        }
        return false;
    }

    private void updateFileInfos(Document xml)
    {
        Element docFiles = xml.getRootElement();
        Element infosFiles = docFiles.element("files");

        List ListFile =  infosFiles.elements("file");
        for (int i = 0; i < ListFile.size(); i++)
        {
            Element el = (Element)ListFile.get(i);
            String defaultAction = el.attributeValue("defaultAction");
            String language = el.attributeValue("language");
            if (language==null)
             language = "";
            String docName = el.getStringValue();
            setDocumentDefaultAction(docName, language, Integer.parseInt(defaultAction));
        }
    }

    private void setDocumentDefaultAction(String docName, String language, int defaultAction)
    {
        if (files == null)
            return;
        for(int i = 0; i < files.size(); i++)
        {
            DocumentInfo di = (DocumentInfo)files.get(i);
            if (di.getFullName().equals(docName) && di.getLanguage().equals(language))
            {
                di.setAction(defaultAction);
                return;
            }
        }
    }

    public int testInstall(XWikiContext context)
    {
        if (log.isDebugEnabled())
            log.debug("Package test install");

        int result = DocumentInfo.INSTALL_IMPOSSIBLE;
        try {
            if (files.size() == 0)
                return result;

            result = ((DocumentInfo)files.get(0)).testInstall(context);
            for (int i = 1; i < files.size(); i++)
            {
                DocumentInfo docInfo = ((DocumentInfo)files.get(i));
                int res = docInfo.testInstall(context);
                if (res < result)
                    result = res;
            }
            return result;
        } finally {
            if (log.isDebugEnabled())
                log.debug("Package test install result " + result);
        }
    }



    public int install(XWikiContext context) throws XWikiException {
        if (testInstall(context) == DocumentInfo.INSTALL_IMPOSSIBLE) {
            return DocumentInfo.INSTALL_IMPOSSIBLE;
        }

        boolean hasCustomMappings = false;
        for (int j = 0;j<customMappingFiles.size();j++)
        {
            DocumentInfo docinfo = (DocumentInfo)files.get(j);
            BaseClass bclass = docinfo.getDoc().getxWikiClass();
            hasCustomMappings |= context.getWiki().getStore().injectCustomMapping(bclass, context);
        }

        if (hasCustomMappings)
            context.getWiki().getStore().injectUpdatedCustomMappings(context);

        for (int i = 0; i < files.size(); i++)
        {
            installDocument(((DocumentInfo)files.get(i)),context);
        }
        return DocumentInfo.INSTALL_OK;
    }

    private int installDocument(DocumentInfo doc, XWikiContext context) throws XWikiException {
        int result = DocumentInfo.INSTALL_OK;

        if (log.isDebugEnabled())
         log.debug("Package installing document " + doc.getFullName() + " " + doc.getLanguage());

        if (doc.getAction() == DocumentInfo.ACTION_SKIP_INSTALL)
            return  DocumentInfo.INSTALL_OK;

        int status = doc.testInstall(context);
        if (status == DocumentInfo.INSTALL_IMPOSSIBLE) {
            return DocumentInfo.INSTALL_IMPOSSIBLE;
        }
        if (status == DocumentInfo.INSTALL_OK || status == DocumentInfo.INSTALL_ALREADY_EXIST && doc.getAction() == DocumentInfo.ACTION_OVERWRITE)
        {
            if (status == DocumentInfo.INSTALL_ALREADY_EXIST)
            {
                XWikiDocument deleteddoc = context.getWiki().getDocument(doc.getFullName(), context);
                try {
                context.getWiki().deleteDocument(deleteddoc, context);
                } catch (Exception e) {
                    // let's log the error but not stop
                    result = DocumentInfo.INSTALL_ERROR;
                    if (log.isErrorEnabled())
                     log.error("Failed to delete document " + deleteddoc.getFullName());
                    if (log.isDebugEnabled())
                     log.debug("Failed to delete document " + deleteddoc.getFullName(), e);
                }
            }
            try {
                if (!backupPack)
                 doc.getDoc().setAuthor(context.getUser());

                // We don't want date and version to change
                // So we need to cancel the dirty status
                // TODO check pb with version
                doc.getDoc().setContentDirty(false);
                doc.getDoc().setMetaDataDirty(false);

                context.getWiki().saveDocument(doc.getDoc(), context);
                doc.getDoc().saveAllAttachments(context);
            } catch (XWikiException e) {
                if (log.isErrorEnabled())
                 log.error("Failed to save document " + doc.getFullName());
                if (log.isDebugEnabled())
                 log.debug("Failed to save document " + doc.getFullName(), e);
                result = DocumentInfo.INSTALL_ERROR;
            }
        }
        return result;
    }


    private String readFromInputStream(InputStream is) throws IOException {
        byte[] data = new byte[4096];
        StringBuffer buffer = new StringBuffer();
        int Cnt;
        while ((Cnt = is.read(data, 0, 4096)) != -1) {
          buffer.append(new String(data, 0, Cnt)) ;
        }
        return buffer.toString();
    }

    private XWikiDocument readFromXML(String XmlFile) throws XWikiException {
        XWikiDocument doc = new com.xpn.xwiki.doc.XWikiDocument();
        if (backupPack && withVersions)
            doc.fromXML(XmlFile, true);
        else {
            doc.fromXML(XmlFile);
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
                description = readPackage(zis);
                return description;
            }
        }
        return null;
    }

    private Document readPackage(InputStream is) throws IOException, DocumentException{
        byte[] data = new byte[4096];
        StringBuffer XmlFile = new StringBuffer();
        int Cnt;
        while ((Cnt = is.read(data, 0, 4096)) != -1) {
          XmlFile.append(new String(data, 0, Cnt)) ;
        }
        return fromXml(XmlFile.toString());
    }

    public String toXml(XWikiContext context)
    {
       OutputFormat outputFormat = new OutputFormat("", true);
        outputFormat.setEncoding(context.getWiki().getEncoding());
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

    private Document toXmlDocument()
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
            DocumentInfo di = (DocumentInfo) files.get(i);
            elfile.addAttribute("defaultAction", String.valueOf(di.getAction()));
            elfile.addAttribute("language", String.valueOf(di.getLanguage()));
            elfile.addText(((DocumentInfo)(files.get(i))).getFullName());
            elfiles.add(elfile);
        }
        return doc;
    }

    private void addInfosToZip(ZipOutputStream zos, XWikiContext context) {
        try  {
        String zipname = DefaultPackageFileName;
        ZipEntry zipentry = new ZipEntry(zipname);
        zos.putNextEntry(zipentry);
        zos.write(toXml(context).getBytes(context.getWiki().getEncoding()));
        zos.closeEntry();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addToZip(XWikiDocument doc, ZipOutputStream zos, boolean withVersions, XWikiContext context) throws IOException {
        try  {
            String zipname = doc.getWeb() + "/" + doc.getName();
            String language = doc.getLanguage();
            if ((language!=null)&&(!language.equals("")))
                zipname += "." + language;
            ZipEntry zipentry = new ZipEntry(zipname);
            zos.putNextEntry(zipentry);
            zos.write(doc.toXML(true, false, true, withVersions, context).getBytes(context.getWiki().getEncoding()));
            zos.closeEntry();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addToDir(XWikiDocument doc, File dir, boolean withVersions, XWikiContext context) throws XWikiException {
        try  {
            filter(doc, context);
            File spacedir = new File(dir, doc.getWeb());
            if (!spacedir.exists()) {
                if (!spacedir.mkdirs()) {
                    Object[] args = new Object[1];
                    args[0] = dir.toString();
                    throw new XWikiException(XWikiException.MODULE_XWIKI ,XWikiException.ERROR_XWIKI_MKDIR, "Error creating directory {0}", null, args);
                }
            }
            String filename = doc.getName();
            String language = doc.getLanguage();
            if ((language!=null)&&(!language.equals("")))
                filename += "." + language;
            File file = new File(spacedir, filename);
            String xml = doc.toXML(true, false, true, withVersions, context);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(xml.getBytes(context.getWiki().getEncoding()));
            fos.flush();
            fos.close();
        } catch (ExcludeDocumentException e) {
            log.info("Skip the document " + doc.getFullName());
        }
        catch (Exception e) {
            Object[] args = new Object[1];
            args[0] = doc.getFullName();
            throw new XWikiException(XWikiException.MODULE_XWIKI_DOC,XWikiException.ERROR_XWIKI_DOC_EXPORT, "Error creating file {0}", e, args);
        }
    }


    private void addInfosToDir(File dir, XWikiContext context) {
        try  {
        String filename = DefaultPackageFileName;
        File file = new File(dir, filename);
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(toXml(context).getBytes(context.getWiki().getEncoding()));
        fos.flush();
        fos.close();
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

    protected void readDependencies()
    {

    }

    public void addAllWikiDocuments(XWikiContext context) throws XWikiException {
        XWiki wiki = context.getWiki();
        List spaces = wiki.getSpaces(context);
        name = "Backup";
        description = "on " + (new Date().toString()) + " by " + context.getUser();
        for(int i = 0; i < spaces.size(); i++)
        {
            List DocsName = wiki.getSpaceDocsName((String) spaces.get(i), context);
            for(int j = 0; j < DocsName.size(); j++) {
                this.add(spaces.get(i) + "." + DocsName.get(j), DocumentInfo.ACTION_OVERWRITE,  context);
            }
        }
        this.backupPack = true;
    }

    public void deleteAllWikiDocuments(XWikiContext context) throws XWikiException {
        XWiki wiki = context.getWiki();
        List spaces = wiki.getSpaces(context);
        for(int i = 0; i < spaces.size(); i++)
        {
            List DocsName = wiki.getSpaceDocsName((String) spaces.get(i), context);
            for(int j = 0; j < DocsName.size(); j++) {
                String docName = spaces.get(i) + "." + DocsName.get(j);
                XWikiDocument doc = wiki.getDocument(docName, context);
                wiki.deleteAllDocuments(doc, context);
            }
        }
    }


    public String readFromDir(File dir, XWikiContext context) throws IOException, XWikiException {
        Document description = null;
        setBackupPack(true);
        int count = 0;
        try {
            File infofile = new File(dir, DefaultPackageFileName);
            description = readPackage(new FileInputStream(infofile));
            if (description==null) {
                throw new PackageException(PackageException.ERROR_PACKAGE_NODESCRIPTION, "Cannot read package description file");
            }

            Element docFiles = description.getRootElement();
            Element infosFiles = docFiles.element("files");

            List ListFile =  infosFiles.elements("file");
            if (log.isInfoEnabled())
             log.info("Package declares " + ListFile.size() + " documents");
            for (int i = 0; i < ListFile.size(); i++)
            {
                Element el = (Element)ListFile.get(i);
                String defaultAction = el.attributeValue("defaultAction");
                String language = el.attributeValue("language");
                if (language==null)
                 language = "";
                String docName = el.getStringValue();
                XWikiDocument doc = new XWikiDocument();
                doc.setFullName(docName, context);
                doc.setLanguage(language);

                if (log.isDebugEnabled())
                 log.debug("Package adding document " + docName + " with language " + language);

                File space = new File(dir, doc.getWeb());
                String filename = doc.getName();
                if ((doc.getLanguage()!=null)&&(!doc.getLanguage().equals("")))
                 filename += "." + doc.getLanguage();
                File docfile = new File(space, filename);
                doc = readFromXML(readFromInputStream(new FileInputStream(docfile)));
                if (doc==null) {
                    if (log.isErrorEnabled())
                     log.info("Package readFrom XML read null doc for " + docName + " language " + language);
                }
                DocumentInfo di = new DocumentInfo(doc);
                di.setAction(Integer.parseInt(defaultAction));
                files.add(di);
                count++;
            }
        } catch (DocumentException e) {
            throw new PackageException(PackageException.ERROR_PACKAGE_UNKNOWN, "Error when reading the XML");
        }
        if (log.isInfoEnabled())
         log.info("Package read " + count + " documents");
        return "";
    }
}

