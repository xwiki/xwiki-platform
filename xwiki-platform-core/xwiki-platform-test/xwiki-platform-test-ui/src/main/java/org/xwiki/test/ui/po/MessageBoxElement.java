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
package org.xwiki.test.ui.po;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Represents a message box displayed in the content of a page, such as the ones rendered by the {@code info},
 * {@code warning}, {@code error} and {@code success} macros.
 *
 * @version $Id$
 * @since 17.10.14
 * @since 18.4.7
 * @since 18.9.0RC1
 */
public class MessageBoxElement extends BaseElement
{
    /**
     * The kinds of message box.
     */
    public enum Type
    {
        /** An information message. */
        INFO("infomessage"),

        /** A warning message. */
        WARNING("warningmessage"),

        /** An error message. */
        ERROR("errormessage"),

        /** A success message. */
        SUCCESS("successmessage");

        private final String cssClass;

        Type(String cssClass)
        {
            this.cssClass = cssClass;
        }
    }

    /**
     * Matches the message boxes of all types.
     */
    static final By LOCATOR = By.cssSelector(
        String.join(", ", Arrays.stream(Type.values()).map(type -> ".box." + type.cssClass).toList()));

    /**
     * The accessible name of the icon, placed by the message macro right after the icon and hidden visually.
     */
    private static final By ICON_NAME = By.xpath("./*[contains(concat(' ', @class, ' '), ' sr-only ')]");

    private final WebElement container;

    /**
     * @param container the element holding the message box
     */
    public MessageBoxElement(WebElement container)
    {
        this.container = container;
    }

    /**
     * @return the kind of this message box
     */
    public Type getType()
    {
        String classes = " " + this.container.getDomAttribute("class") + " ";
        return Arrays.stream(Type.values()).filter(type -> classes.contains(" " + type.cssClass + " ")).findFirst()
            .orElseThrow(() -> new IllegalStateException("Unknown message box type: " + classes.trim()));
    }

    /**
     * @return the message displayed in this message box, including its title, if any, but without the accessible
     *         name of its icon (such as "Information"), which is only read by screen readers
     */
    public String getText()
    {
        String text = this.container.getText();
        for (WebElement iconName : getDriver().findElementsWithoutWaiting(this.container, ICON_NAME)) {
            text = StringUtils.removeStart(text, iconName.getText()).strip();
        }
        return text;
    }

    /**
     * @param text the exact text of a formatted fragment of this message box, such as a word
     * @return {@code true} if a fragment whose text is exactly the given text is displayed in bold, {@code false}
     *         otherwise (a longer bold fragment containing the given text doesn't match)
     */
    public boolean isBold(String text)
    {
        return isFormatted(text, "strong, b");
    }

    /**
     * @param text the exact text of a formatted fragment of this message box, such as a word
     * @return {@code true} if a fragment whose text is exactly the given text is displayed in italic, {@code false}
     *         otherwise (a longer italic fragment containing the given text doesn't match)
     */
    public boolean isItalic(String text)
    {
        return isFormatted(text, "em, i");
    }

    /**
     * @param text the exact text of a formatted fragment of this message box, such as a word
     * @return {@code true} if a fragment whose text is exactly the given text is displayed underlined, {@code false}
     *         otherwise (a longer underlined fragment containing the given text doesn't match)
     */
    public boolean isUnderlined(String text)
    {
        return isFormatted(text, "ins, u");
    }

    private boolean isFormatted(String text, String formattingElements)
    {
        return getDriver().findElementsWithoutWaiting(this.container, By.cssSelector(formattingElements)).stream()
            .anyMatch(element -> text.equals(element.getText()));
    }
}
