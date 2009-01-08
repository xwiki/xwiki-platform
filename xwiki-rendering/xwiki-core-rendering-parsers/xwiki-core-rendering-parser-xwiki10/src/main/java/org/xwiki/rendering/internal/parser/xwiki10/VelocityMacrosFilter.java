package org.xwiki.rendering.internal.parser.xwiki10;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Composable;
import org.xwiki.rendering.parser.xwiki10.AbstractFilter;
import org.xwiki.rendering.parser.xwiki10.FilterContext;
import org.xwiki.rendering.parser.xwiki10.macro.VelocityMacroConverter;
import org.xwiki.rendering.parser.xwiki10.util.CleanUtil;

/**
 * Convert 1.0 velocity macros to 2.0 macro. A conversion can be added by implementing VelocityMacroConverter.
 * 
 * @version $Id$
 */
public class VelocityMacrosFilter extends AbstractFilter implements Composable
{
    /**
     * Regex pattern for matching macros that are written on single line.
     */
    public static final Pattern VELOCITY_MACRO_PATTERN = Pattern.compile("\\#(\\w+)\\(([^)]*)\\)");

    private ComponentManager componentManager;

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.component.phase.Composable#compose(org.xwiki.component.manager.ComponentManager)
     */
    public void compose(ComponentManager componentManager)
    {
        this.componentManager = componentManager;
    }

    public String filter(String content, FilterContext filterContext)
    {
        content = filterMacros(content, filterContext);

        return content;
    }

    private String filterMacros(String content, FilterContext filterContext)
    {
        StringBuffer result = new StringBuffer();
        Matcher matcher = VELOCITY_MACRO_PATTERN.matcher(content);

        int currentIndex = 0;
        VelocityMacroConverter currentMacro = null;
        for (; matcher.find(); currentIndex = matcher.end()) {
            String before = content.substring(currentIndex, matcher.start());

            if (currentMacro != null && !currentMacro.isInline()) {
                before = CleanUtil.setFirstNL(before, 2);
            }

            String allcontent = matcher.group(0);

            String macroName = matcher.group(1);
            String params = matcher.group(2);

            try {
                currentMacro =
                    (VelocityMacroConverter) this.componentManager.lookup(VelocityMacroConverter.ROLE, macroName);

                // a standalone new line is not interpreted by XWiki 1.0 rendering
                before = CleanUtil.removeLastStandaloneNewLine(before);

                if (!currentMacro.isInline()) {
                    before = CleanUtil.setLastNL(before, 2);
                }

                allcontent = currentMacro.convert(macroName, getMacroParameters(params));
                if (currentMacro.protectResult()) {
                    allcontent = filterContext.addProtectedContent(allcontent);
                }
            } catch (ComponentLookupException e) {
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("Can't find macro converter [" + macroName + "]", e);
                }

                currentMacro = null;
            }

            result.append(before);
            result.append(allcontent);
        }

        if (currentIndex == 0) {
            return content;
        }

        if (currentMacro != null && !currentMacro.isInline()) {
            result.append(CleanUtil.setFirstNL(content.substring(currentIndex), 2));
        } else {
            result.append(content.substring(currentIndex));
        }

        return result.toString();
    }

    private List<String> getMacroParameters(String parameters)
    {
        List<String> parameterList = new ArrayList<String>();

        if (parameters != null) {
            String[] parameterTable = parameters.split(" ");

            for (String parameterValue : parameterTable) {
                if (parameterValue.length() > 0) {
                    parameterList.add(parameterValue);
                }
            }
        }

        return parameterList;
    }
}
