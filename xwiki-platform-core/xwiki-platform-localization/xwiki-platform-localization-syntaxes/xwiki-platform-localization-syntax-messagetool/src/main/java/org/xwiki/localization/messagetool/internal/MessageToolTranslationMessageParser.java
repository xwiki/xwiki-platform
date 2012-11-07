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
package org.xwiki.localization.messagetool.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.localization.message.TranslationMessage;
import org.xwiki.localization.message.TranslationMessageParser;
import org.xwiki.rendering.parser.Parser;

/**
 * @version $Id$
 * @since 4.3M2
 */
@Component
@Singleton
@Named(MessageToolTranslationMessageParser.HINT)
public class MessageToolTranslationMessageParser implements TranslationMessageParser
{
    /**
     * The role hint of the component.
     */
    public static final String HINT = "messagetool/1.0";

    /**
     * The plain text parser.
     */
    @Inject
    @Named("plain/1.0")
    private Parser plainParser;

    @Override
    public TranslationMessage parse(String message)
    {
        TranslationMessage translationMessage;

        if (StringUtils.isEmpty(message)) {
            translationMessage = TranslationMessage.EMPTY;
        } else {
            translationMessage = new MessageFormatTranslationMessage(message, this.plainParser);
        }

        return translationMessage;
    }
}
