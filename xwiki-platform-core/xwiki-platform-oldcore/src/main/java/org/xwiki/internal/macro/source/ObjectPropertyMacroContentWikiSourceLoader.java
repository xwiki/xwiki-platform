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
package org.xwiki.internal.macro.source;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.mail.GeneralMailConfiguration;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.source.MacroContentSourceReference;
import org.xwiki.rendering.macro.source.MacroContentWikiSource;
import org.xwiki.rendering.syntax.Syntax;

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
 * @since 15.1RC1
 * @since 14.10.5
 */
@Component(hints = {"OBJECT_PROPERTY", "PAGE_OBJECT_PROPERTY"})
@Singleton
public class ObjectPropertyMacroContentWikiSourceLoader implements EntityMacroContentWikiSourceLoader
{
    @Inject
    private GeneralMailConfiguration mailConfiguration;

    @Override
    public MacroContentWikiSource load(XWikiDocument document, EntityReference entityReference,
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

        return new MacroContentWikiSource(reference, xobjectProperty.toText(),
            getSyntax(xobject, document, entityReference, xcontext));
    }

    private Syntax getSyntax(BaseObject xobject, XWikiDocument document, EntityReference entityReference,
        XWikiContext xcontext) throws MacroExecutionException
    {
        BaseClass xclass = xobject.getXClass(xcontext);

        if (xclass != null) {
            PropertyInterface xclassProperty = xclass.get(entityReference.getName());

            // Displaying a password is forbidden
            if (xclassProperty instanceof PasswordClass) {
                throw new MacroExecutionException(String.format(
                    "Displaying content of property [%s] is not allowed because it's a password", entityReference));
            }

            // Displaying email is forbidden when obfuscation is enabled
            if (xclassProperty instanceof EmailClass && this.mailConfiguration.shouldObfuscate()) {
                throw new MacroExecutionException(
                    String.format("Displaying content of property [%s] is not allowed because it's an obfuscated email",
                        entityReference));
            }

            // We are not supposed to interpret wiki syntax in an object property which is not a wiki textarea so
            // default to plain text syntax
            Syntax syntax = Syntax.PLAIN_1_0;
            if (xclassProperty instanceof TextAreaClass) {
                TextAreaClass textarea = (TextAreaClass) xclassProperty;

                ContentType contentType = ContentType.getByValue(textarea.getContentType());
                if (contentType == ContentType.WIKI_TEXT) {
                    syntax = document.getSyntax();
                }
            }

            return syntax;
        }

        return null;
    }
}
