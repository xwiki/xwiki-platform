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
package org.xwiki.internal.web;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.GetAction;
import com.xpn.xwiki.web.XWikiRequest;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

/**
 * Implementation of {@link DocExistValidator} for the {@code get} actions.
 *
 * @version $Id$
 * @since 13.10.4
 * @since 14.2RC1
 */
@Component
@Singleton
@Named(GetAction.GET_ACTION)
public class GetDocExistValidator implements DocExistValidator
{
    @Inject
    @Named("currentmixed")
    private DocumentReferenceResolver<String> currentmixedReferenceResolver;

    @Inject
    private Logger logger;

    @Override
    public boolean docExist(XWikiDocument doc, XWikiContext context)
    {
        boolean result = false;
        if (doc.isNew()) {
            XWikiRequest request = context.getRequest();
            String sheet = request.get("sheet");
            if (!StringUtils.isEmpty(sheet)) {
                DocumentReference sheetReference = this.currentmixedReferenceResolver.resolve(sheet);
                try {
                    XWikiDocument sheetDoc = context.getWiki().getDocument(sheetReference, context);
                    result = sheetDoc.isNew();
                } catch (XWikiException e) {
                    this.logger.warn(
                        "Error while trying to load sheet [{}] for checking status code on GET request for "
                            + "[{}]: [{}]", sheetReference, doc.getDocumentReference(), getRootCauseMessage(e));
                    // There is an error, we consider that the sheet doesn't exist.
                    result = true;
                }
            } else {
                result = !"1".equals(request.get("disableCheckNotExisting"));
            }
        }
        return result;
    }
}
