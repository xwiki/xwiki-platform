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
package com.xpn.xwiki.render.filter;

import java.io.Writer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.radeox.api.engine.context.InitialRenderContext;
import org.radeox.filter.context.FilterContext;
import org.radeox.filter.regex.RegexTokenFilter;
import org.radeox.macro.Macro;
import org.radeox.macro.Repository;
import org.radeox.macro.parameter.MacroParameter;
import org.radeox.regex.MatchResult;
import org.radeox.util.StringBufferWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.render.macro.MacroRepository;

public class StyleFilter extends RegexTokenFilter
{
    private static Logger LOGGER = LoggerFactory.getLogger(MacroFilter.class);

    private MacroRepository macros;

    public StyleFilter()
    {
        super("\\{(style)(?::([^\\}]*))?\\}(.*)\\{style}", MULTILINE);
    }

    public void setInitialContext(InitialRenderContext context)
    {
        macros = MacroRepository.getInstance();
        macros.setInitialContext(context);
    }

    protected Repository getMacroRepository()
    {
        return macros;
    }

    public void handleMatch(StringBuffer buffer, MatchResult result, FilterContext context)
    {
        String command = result.group(1);

        if (command != null) {
            // {$peng} are variables not macros.
            if (!command.startsWith("$")) {
                MacroParameter mParams = context.getMacroParameter();
                switch (result.groups()) {
                    case 3:
                        mParams.setContent(result.group(3));
                        mParams.setContentStart(result.beginOffset(3));
                        mParams.setContentEnd(result.endOffset(3));
                    case 2:
                        mParams.setParams(result.group(2));
                }
                mParams.setStart(result.beginOffset(0));
                mParams.setEnd(result.endOffset(0));

                // @DANGER: recursive calls may replace macros in included source code
                try {
                    if (command.equals("style") && (mParams.getContent() != null)) {
                        // We need to handle recursivity here
                        String content = mParams.getContent();
                        Pattern pattern = Pattern.compile("\\{" + command + ".*?\\}");

                        // This code allows to find the real end tag
                        Matcher matcher = pattern.matcher(content);
                        int startTagNumber = 1;
                        int endPosition = content.length();
                        while (matcher.find()) {
                            String match = matcher.group();
                            if (match.equals("{" + command + "}")) {
                                startTagNumber--;
                                if (startTagNumber == 0) {
                                    endPosition = matcher.start();
                                    break;
                                }
                            } else {
                                startTagNumber++;
                            }
                        }

                        // Get the content up to the real end tag
                        String realContent = content.substring(0, endPosition);

                        // Execute any nested macros and filters
                        mParams.setContent(realContent);
                        Writer writer = new StringBufferWriter(buffer);
                        Macro macro = (Macro) getMacroRepository().get(command);
                        // Execute the macro resulting content
                        macro.execute(writer, mParams);

                        if (content.length() != endPosition) {
                            // Get the content after the real end tag
                            String nextContent = content.substring(endPosition + 2 + command.length()) + "{style}";
                            // Execute other macros on content after the real end tag
                            writer.append(nextContent);
                        }
                    } else if (getMacroRepository().containsKey(command)) {
                        Macro macro = (Macro) getMacroRepository().get(command);
                        // recursively filter macros within macros
                        if (null != mParams.getContent()) {
                            mParams.setContent(mParams.getContent());
                        }

                        Writer writer = new StringBufferWriter(buffer);
                        macro.execute(writer, mParams);
                    } else {
                        buffer.append(result.group(0));
                        return;
                    }
                } catch (IllegalArgumentException e) {
                    buffer.append("<div class=\"error\">" + command + ": " + e.getMessage() + "</div>");
                } catch (Throwable e) {
                    LOGGER.warn("MacroFilter: unable to format macro: " + result.group(1), e);
                    buffer.append("<div class=\"error\">" + command + ": " + e.getMessage() + "</div>");
                    return;
                }
            } else {
                buffer.append("<");
                buffer.append(command.substring(1));
                buffer.append(">");
            }
        } else {
            buffer.append(result.group(0));
        }
    }
}
