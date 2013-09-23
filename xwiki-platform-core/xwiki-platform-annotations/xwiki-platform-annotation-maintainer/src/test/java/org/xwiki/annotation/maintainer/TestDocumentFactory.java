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
package org.xwiki.annotation.maintainer;

import java.io.IOException;

import org.xwiki.annotation.Annotation;

/**
 * Factory for loading mock documents from test files extended for loading documents with updates.
 * 
 * @version $Id$
 * @since 2.3M1
 */
public class TestDocumentFactory extends org.xwiki.annotation.TestDocumentFactory
{
    @Override
    public MockDocument getDocument(String docName) throws IOException
    {
        MockDocument loadedDoc = (MockDocument) docs.get(docName);
        if (loadedDoc == null) {
            loadedDoc = new MockDocument();
            loadDocument(loadedDoc, docName);
            docs.put(docName, loadedDoc);
        }
        return loadedDoc;
    }

    @Override
    protected void saveKeyToDoc(String currentKey, String currentValue, org.xwiki.annotation.MockDocument doc,
        String docName) throws IOException
    {
        // test if it's a modified annotation and parse & save as a modified annotation
        if (currentKey.equals("annotationUpdated") && doc instanceof MockDocument) {
            Annotation ann = parseAnnotation(currentValue, docName);
            ((MockDocument) doc).getUpdatedAnnotations().add(ann);
        } else {
            super.saveKeyToDoc(currentKey, currentValue, doc, docName);
        }
    }
}
