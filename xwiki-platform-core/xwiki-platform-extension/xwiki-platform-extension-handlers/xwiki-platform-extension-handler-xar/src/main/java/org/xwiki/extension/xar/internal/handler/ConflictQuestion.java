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
package org.xwiki.extension.xar.internal.handler;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * 
 * @version $Id$
 * @since 4.0M2
 */
public class ConflictQuestion
{
    public static enum GlobalAction
    {
        CURRENT,
        PREVIOUS,
        NEXT,
        MERGED,
        CUSTOM
    };

    // Answer datas

    private GlobalAction globalAction = GlobalAction.MERGED;

    private XWikiDocument customDocument;

    // Question datas

    private XWikiDocument currentDocument;

    private XWikiDocument previousDocument;

    private XWikiDocument nextDocument;

    private XWikiDocument mergedDocument;

    public ConflictQuestion(XWikiDocument currentDocument, XWikiDocument previousDocument, XWikiDocument nextDocument,
        XWikiDocument mergedDocument)
    {
        this.currentDocument = currentDocument;
        this.previousDocument = previousDocument;
        this.nextDocument = nextDocument;
        this.mergedDocument = mergedDocument;
    }

    public XWikiDocument getCurrentDocument()
    {
        return this.currentDocument;
    }

    public XWikiDocument getPreviousDocument()
    {
        return this.previousDocument;
    }

    public XWikiDocument getNextDocument()
    {
        return this.nextDocument;
    }

    public XWikiDocument getMergedDocument()
    {
        return this.mergedDocument;
    }

    // Answer

    public GlobalAction getGlobalAction()
    {
        return this.globalAction;
    }

    public void setGlobalAction(GlobalAction globalAction)
    {
        this.globalAction = globalAction;
    }

    public XWikiDocument getCustomDocument()
    {
        return this.customDocument;
    }

    public void setCurrentDocument(XWikiDocument currentDocument)
    {
        this.currentDocument = currentDocument;
    }
}
