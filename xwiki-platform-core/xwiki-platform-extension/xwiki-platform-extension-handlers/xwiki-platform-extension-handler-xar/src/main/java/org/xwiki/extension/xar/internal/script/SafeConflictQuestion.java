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
package org.xwiki.extension.xar.internal.script;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.xwiki.extension.xar.question.ConflictQuestion;
import org.xwiki.extension.xar.question.ConflictQuestion.GlobalAction;
import org.xwiki.script.safe.AbstractSafeObject;
import org.xwiki.script.safe.ScriptSafeProvider;

import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @version $Id$
 * @since 4.0M2
 */
public class SafeConflictQuestion extends AbstractSafeObject<ConflictQuestion>
{
    /**
     * @param question the wrapped object
     * @param safeProvider the provider of instances safe for public scripts
     */
    public SafeConflictQuestion(ConflictQuestion question, ScriptSafeProvider< ? > safeProvider)
    {
        super(question, safeProvider);
    }

    // Datas

    public Document getCurrentDocument()
    {
        return safe(getWrapped().getCurrentDocument());
    }

    public Document getPreviousDocument()
    {
        return safe(getWrapped().getPreviousDocument());
    }

    public Document getNextDocument()
    {
        return safe(getWrapped().getNextDocument());
    }

    public Document getMergedDocument()
    {
        return safe(getWrapped().getMergedDocument());
    }

    // Answer

    public GlobalAction getGlobalAction()
    {
        return getWrapped().getGlobalAction();
    }

    public void setGlobalAction(GlobalAction globalAction)
    {
        getWrapped().setGlobalAction(globalAction);
    }

    public Document getCustomDocument()
    {
        return safe(getWrapped().getCustomDocument());
    }

    /**
     * @since 4.1
     */
    public void setCustomDocument(Document customDocument)
    {
        try {
            XWikiDocument document = (XWikiDocument) FieldUtils.readField(customDocument, "doc", true);

            getWrapped().setCustomDocument(document);
        } catch (IllegalAccessException e) {
            // TODO: log something ?
        }
    }
}
