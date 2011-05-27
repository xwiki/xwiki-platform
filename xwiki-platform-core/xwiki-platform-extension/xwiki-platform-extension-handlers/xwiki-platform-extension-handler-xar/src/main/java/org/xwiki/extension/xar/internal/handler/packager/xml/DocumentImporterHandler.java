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
package org.xwiki.extension.xar.internal.handler.packager.xml;

import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.ObjectUtils;
import org.suigeneris.jrcs.diff.Diff;
import org.suigeneris.jrcs.diff.Revision;
import org.suigeneris.jrcs.util.ToString;
import org.xml.sax.SAXException;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.extension.xar.internal.handler.packager.DefaultPackager;
import org.xwiki.extension.xar.internal.handler.packager.NotADocumentException;
import org.xwiki.extension.xar.internal.handler.packager.XarEntry;
import org.xwiki.extension.xar.internal.handler.packager.XarFile;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.AttachmentDiff;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.ObjectDiff;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;

public class DocumentImporterHandler extends DocumentHandler
{
    private XarFile previousXarFile;

    private DefaultPackager packager;

    public DocumentImporterHandler(DefaultPackager packager, ComponentManager componentManager, String wiki)
    {
        super(componentManager, wiki);

        this.packager = packager;
    }

    public void setPreviousXarFile(XarFile previousXarFile)
    {
        this.previousXarFile = previousXarFile;
    }

    private void saveDocument(String comment) throws SAXException
    {
        try {
            XWikiContext context = getXWikiContext();

            XWikiDocument document = getDocument();
            XWikiDocument dbDocument = getDatabaseDocument().clone();
            XWikiDocument previousDocument = getPreviousDocument();

            if (previousDocument != null) {
                if (merge(previousDocument, document, dbDocument).isModified()) {
                    context.getWiki().saveDocument(document, comment, context);
                }
            } else {
                if (!dbDocument.isNew()) {
                    document.setVersion(dbDocument.getVersion());
                }

                context.getWiki().saveDocument(document, comment, context);
            }
        } catch (Exception e) {
            throw new SAXException("Failed to save document", e);
        }
    }

    private MergeResult merge(XWikiDocument document1, XWikiDocument document2, XWikiDocument documentResult)
    {
        MergeResult mergeResult = new MergeResult();

        XWikiContext context;
        try {
            context = getXWikiContext();
        } catch (ComponentLookupException e) {
            mergeResult.getErrors().add(e);

            return mergeResult;
        }

        // Title
        documentResult.setTitle(mergeString(document1.getTitle(), document2.getTitle(), documentResult.getTitle(),
            mergeResult));

        // Content
        documentResult.setContent(mergeString(document1.getContent(), document2.getContent(),
            documentResult.getContent(), mergeResult));

        // Parent
        // if (!ObjectUtils.equals(document1.getAuthorReference(), document2.getAuthorReference())
        // && ObjectUtils.equals(document1.getAuthorReference(), result.getAuthorReference())) {
        // result.setParentReference(document2.getParentReference());
        //
        // mergeResult.setModified(true);
        // }

        // Author
        if (!ObjectUtils.equals(document1.getAuthorReference(), document2.getAuthorReference())
            && ObjectUtils.equals(document1.getAuthorReference(), documentResult.getAuthorReference())) {
            documentResult.setAuthorReference(document2.getAuthorReference());

            mergeResult.setModified(true);
        }

        // Objects
        try {
            List<List<ObjectDiff>> objectsDiff = document1.getObjectDiff(document1, document2, context);
            if (!objectsDiff.isEmpty()) {
                // Apply diff on result
                for (List<ObjectDiff> objectClassDiff : objectsDiff) {
                    for (ObjectDiff diff : objectClassDiff) {
                        BaseObject objectResult =
                            documentResult.getXObject(diff.getXClassReference(), diff.getNumber());
                        BaseObject object1 = document1.getXObject(diff.getXClassReference(), diff.getNumber());
                        BaseObject object2 = document2.getXObject(diff.getXClassReference(), diff.getNumber());
                        PropertyInterface propertyResult =
                            objectResult != null ? objectResult.getField(diff.getPropName()) : null;
                        PropertyInterface property1 =
                            object1 != null ? objectResult.getField(diff.getPropName()) : null;
                        PropertyInterface property2 =
                            object2 != null ? objectResult.getField(diff.getPropName()) : null;

                        if (diff.getAction() == ObjectDiff.ACTION_OBJECTADDED) {
                            if (objectResult != null) {
                                documentResult.setXObject(object2.getNumber(), object2);
                            } else {
                                // XXX: collision between db and new ?
                            }
                        } else if (diff.getAction() == ObjectDiff.ACTION_OBJECTREMOVED) {
                            if (objectResult != null) {
                                if (objectResult.equals(object1)) {
                                    documentResult.removeXObject(objectResult);
                                } else {
                                    // XXX: collision between db and new ?
                                }
                            }
                        } else if (diff.getAction() == ObjectDiff.ACTION_PROPERTYADDED) {
                            if (propertyResult == null) {
                                objectResult.addField(diff.getPropName(), property2);
                            } else {
                                // XXX: collision between db and new ?
                            }
                        } else if (diff.getAction() == ObjectDiff.ACTION_PROPERTYREMOVED) {
                            if (propertyResult != null) {
                                if (propertyResult.equals(property1)) {
                                    objectResult.removeField(diff.getPropName());
                                } else {
                                    // XXX: collision between db and new ?
                                }
                            }
                        } else if (diff.getAction() == ObjectDiff.ACTION_PROPERTYCHANGED) {
                            if (propertyResult != null) {
                                if (propertyResult.equals(property1)) {
                                    // TODO: refactor property
                                } else {
                                    // XXX: collision between db and new ?
                                }
                            }
                        }
                    }
                }

                mergeResult.setModified(true);
            }
        } catch (XWikiException e) {
            mergeResult.getErrors().add(e);
        }

        // Class
        try {
            List<List<ObjectDiff>> classDiff = document1.getClassDiff(document1, document2, context);
            if (!classDiff.isEmpty()) {
                // Apply diff on result
                BaseClass classResult = documentResult.getXClass();
                BaseClass class1 = document1.getXClass();
                BaseClass class2 = document2.getXClass();
                for (ObjectDiff diff : classDiff.get(0)) {
                    PropertyInterface propertyResult = classResult.getField(diff.getPropName());
                    PropertyInterface property1 = class1.getField(diff.getPropName());
                    PropertyInterface property2 = class2.getField(diff.getPropName());

                    if (diff.getAction() == ObjectDiff.ACTION_PROPERTYADDED) {
                        if (propertyResult == null) {
                            // Add if none has been added by user already
                            classResult.addField(diff.getPropName(), class2.getField(diff.getPropName()));
                        } else if (!propertyResult.equals(property2)) {
                            // XXX: collision between db and new ?
                        }
                    } else if (diff.getAction() == ObjectDiff.ACTION_PROPERTYREMOVED) {
                        if (propertyResult != null) {
                            if (propertyResult.equals(property1)) {
                                // Delete if it's the same as previous one
                                classResult.removeField(diff.getPropName());
                            } else {
                                // XXX: collision between db and new ?
                            }
                        }
                    } else if (diff.getAction() == ObjectDiff.ACTION_PROPERTYCHANGED) {
                        if (propertyResult != null) {
                            if (propertyResult.equals(property1)) {
                                // TODO: modify existing class property
                            } else if (!propertyResult.equals(property2)) {
                                // XXX: collision between db and new ?
                            }
                        }
                    }
                }

                mergeResult.setModified(true);
            }
        } catch (XWikiException e) {
            mergeResult.getErrors().add(e);
        }

        // Attachments
        try {
            List<AttachmentDiff> attachmentsDiff = document1.getAttachmentDiff(document1, document2, context);
            if (!attachmentsDiff.isEmpty()) {
                // Apply deleted attachment diff on result (new attachment has already been saved)
                for (AttachmentDiff diff : attachmentsDiff) {
                    if (diff.getNewVersion() == null) {
                        try {
                            XWikiAttachment attachmentResult = documentResult.getAttachment(diff.getFileName());

                            documentResult.deleteAttachment(attachmentResult, context);

                            mergeResult.setModified(true);
                        } catch (XWikiException e) {
                            mergeResult.getErrors().add(e);
                        }
                    }
                }
            }
        } catch (XWikiException e) {
            mergeResult.getErrors().add(e);
        }

        return mergeResult;
    }

    private String mergeString(String str1, String str2, String current, MergeResult mergeResult)
    {
        String result = current;

        try {
            Revision revision = Diff.diff(ToString.stringToArray(str1), ToString.stringToArray(str2));
            if (revision.size() > 0) {
                result = ToString.arrayToString(revision.patch(ToString.stringToArray(current)));

                mergeResult.setModified(true);
            }
        } catch (Exception e) {
            mergeResult.getErrors().add(e);
        }

        return result;
    }

    private XWikiDocument getDatabaseDocument() throws ComponentLookupException, XWikiException
    {
        XWikiContext context = getXWikiContext();
        XWikiDocument document = getDocument();

        XWikiDocument existingDocument = context.getWiki().getDocument(document.getDocumentReference(), context);
        existingDocument = existingDocument.getTranslatedDocument(document.getLanguage(), context);

        return existingDocument;
    }

    private XWikiDocument getPreviousDocument() throws NotADocumentException, ParserConfigurationException,
        SAXException, IOException
    {
        XWikiDocument document = getDocument();

        DocumentHandler documentHandler = new DocumentHandler(getComponentManager(), document.getWikiName());

        this.packager.parseDocument(this.previousXarFile.getInputStream(new XarEntry(document.getSpace(), document
            .getName(), document.getLanguage())), documentHandler);

        return documentHandler.getDocument();
    }

    private void saveAttachment(XWikiAttachment attachment, String comment) throws SAXException
    {
        try {
            XWikiContext context = getXWikiContext();
            XWikiDocument dbDocument = getDatabaseDocument();

            XWikiAttachment dbAttachment = dbDocument.getAttachment(attachment.getFilename());

            if (dbAttachment == null) {
                dbDocument.getAttachmentList().add(attachment);
            } else {
                dbAttachment.setContent(attachment.getContentInputStream(context));
                dbAttachment.setFilename(attachment.getFilename());
                dbAttachment.setAuthor(attachment.getAuthor());
            }

            context.getWiki().saveDocument(dbDocument, comment, context);

            // reset content to since it could consume lots of memory and it's not used in diff for now
            attachment.setAttachment_content(null);
            getDocument().getAttachmentList().add(attachment);
        } catch (Exception e) {
            throw new SAXException("Failed to save attachment [" + attachment + "]", e);
        }
    }

    @Override
    protected void endAttachment(String uri, String localName, String qName) throws SAXException
    {
        AttachmentHandler handler = (AttachmentHandler) getCurrentHandler();

        saveAttachment(handler.getAttachment(), "Import: add attachment");
    }

    @Override
    protected void endHandlerElement(String uri, String localName, String qName) throws SAXException
    {
        saveDocument(getDocument().getAttachmentList().isEmpty() ? "Import" : "Import: final save");
    }
}
