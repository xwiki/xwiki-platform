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
package org.xwiki.rendering.internal.macro.code.source;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.mail.GeneralMailConfiguration;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rendering.internal.parser.pygments.PygmentsUtils;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.code.source.CodeMacroSource;
import org.xwiki.rendering.macro.source.MacroContentSourceReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.EmailClass;
import com.xpn.xwiki.objects.classes.PasswordClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;
import com.xpn.xwiki.objects.classes.TextAreaClass.ContentType;

/**
 * @version $Id$
 * @since 15.0RC1
 * @since 14.10.2
 */
@Component(hints = {"OBJECT_PROPERTY", "PAGE_OBJECT_PROPERTY"})
@Singleton
public class DocumentObjectPropertyCodeMacroSourceLoader implements EntityCodeMacroSourceLoader
{
    @Inject
    private GeneralMailConfiguration mailConfiguration;

    @Override
    public CodeMacroSource load(XWikiDocument document, EntityReference entityReference,
        MacroContentSourceReference reference, XWikiContext xcontext) throws MacroExecutionException
    {
        BaseObject xobject = document.getXObject(entityReference.getParent());
        BaseProperty xobjectProperty;
        try {
            xobjectProperty = (BaseProperty) xobject.get(entityReference.getName());
        } catch (XWikiException e) {
            throw new MacroExecutionException("Failed to access object property [" + entityReference + "]", e);
        }

        if (xobjectProperty == null) {
            throw new MacroExecutionException("Unknown property [" + entityReference + "]");
        }

        return new CodeMacroSource(reference, xobjectProperty.toText(),
            getLanguage(xobject, document, entityReference, xcontext));
    }

    private String getLanguage(BaseObject xobject, XWikiDocument document, EntityReference entityReference,
        XWikiContext xcontext) throws MacroExecutionException
    {
        BaseClass xclass = xobject.getXClass(xcontext);

        if (xclass != null) {
            PropertyInterface xclassProperty = xclass.get(entityReference.getName());

            // Displaying a password is forbidden
            if (xclassProperty instanceof PasswordClass) {
                throw new MacroExecutionException(String.format(
                    "Displaying content of property [%s] is not allowed because it's a passwordl", entityReference));
            }

            // Displaying email is forbidden when obfuscation is enabled
            if (xclassProperty instanceof EmailClass && this.mailConfiguration.shouldObfuscate()) {
                throw new MacroExecutionException(
                    String.format("Displaying content of property [%s] is not allowed because it's an obfuscated email",
                        entityReference));
            }

            String language = null;
            if (xclassProperty instanceof TextAreaClass) {
                TextAreaClass textarea = (TextAreaClass) xclassProperty;

                ContentType contentType = ContentType.getByValue(textarea.getContentType());
                if (contentType == ContentType.VELOCITY_CODE || contentType == ContentType.VELOCITYWIKI) {
                    language = "velocity";
                } else if (contentType == ContentType.WIKI_TEXT) {
                    language = PygmentsUtils.syntaxToLanguage(document.getSyntax());
                }
            }

            return language;
        }

        return null;
    }
}
