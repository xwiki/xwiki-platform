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
package org.xwiki.uiextension.internal.parameters;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import org.apache.commons.lang3.Strings;
import org.xwiki.component.annotation.Component;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.velocity.internal.util.VelocityDetector;
import org.xwiki.webjars.WebJarsUrlFactory;

import com.xpn.xwiki.XWikiContext;

/**
 * Creates the {@link UIXParameterValue} matching a raw UI extension parameter value.
 * <p>
 * Values that consist of literal text around a single call of {@code $services.localization.render},
 * {@code $services.webjars.url} or {@code $xwiki.getSkinFile} with literal string arguments are executed directly in
 * Java, as these calls are the most common Velocity code in UI extension parameters. This ignores any rebinding of
 * {@code $services} or {@code $xwiki} by the current request's scripts. Any other value containing Velocity is
 * evaluated with the Velocity engine.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
@Component(roles = UIXParameterValueFactory.class)
@Singleton
public class UIXParameterValueFactory
{
    private static final String LOCALIZATION_RENDER = "services.localization.render";

    private static final String WEBJARS_URL = "services.webjars.url";

    private static final String SKIN_FILE = "xwiki.getSkinFile";

    /**
     * Matches literal text, a single call and literal text again. The literal text can't contain any {@code $} or
     * {@code #}, and the arguments can't contain any parenthesis to keep the argument parsing simple. Values that
     * don't match fall back to the Velocity engine.
     */
    private static final Pattern CALL_PATTERN = Pattern.compile("^([^$#]*)\\$(%s|%s|%s)\\(([^()$#]*)\\)([^$#]*)$"
        .formatted(Pattern.quote(LOCALIZATION_RENDER), Pattern.quote(WEBJARS_URL), Pattern.quote(SKIN_FILE)));

    /**
     * Matches one string literal argument followed by a comma or the end of the arguments. Single-quoted strings
     * aren't interpolated by Velocity, double-quoted strings without {@code $}, {@code #} and {@code \} are equivalent.
     */
    private static final Pattern ARGUMENT_PATTERN =
        Pattern.compile("\\G\\s*+(?:'([^'$#]*+)'|\"([^\"$#\\\\]*+)\")\\s*+(,|\\z)");

    private static final String COMMA = ",";

    @Inject
    private VelocityDetector velocityDetector;

    @Inject
    private VelocityManager velocityManager;

    @Inject
    private Provider<ContextualLocalizationManager> localizationManagerProvider;

    @Inject
    private Provider<WebJarsUrlFactory> webJarsUrlFactoryProvider;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    /**
     * @param namespace the Velocity namespace to use when the value is evaluated with Velocity, it also identifies
     *     the value in error messages
     * @param raw the raw value of the parameter
     * @return the parameter value
     */
    public UIXParameterValue create(String namespace, String raw)
    {
        UIXParameterValue result;

        if (!this.velocityDetector.containsVelocityScript(raw)) {
            result = new StaticParameterValue(raw);
        } else {
            result = createShortcut(raw);
            if (result == null) {
                result = new VelocityParameterValue(raw, namespace, this.velocityManager);
            }
        }

        return result;
    }

    private UIXParameterValue createShortcut(String raw)
    {
        Matcher matcher = CALL_PATTERN.matcher(raw);
        if (!matcher.matches()) {
            return null;
        }

        String prefix = matcher.group(1);
        String suffix = matcher.group(4);
        // A backslash escapes the reference, and a dot or square bracket would continue it.
        if (prefix.endsWith("\\") || Strings.CS.startsWithAny(suffix, ".", "[")) {
            return null;
        }

        List<String> arguments = parseArguments(matcher.group(3));
        if (arguments == null) {
            return null;
        }

        Supplier<String> call = switch (matcher.group(2)) {
            case LOCALIZATION_RENDER -> getLocalizationCall(arguments);
            case WEBJARS_URL -> getWebJarCall(arguments);
            default -> getSkinFileCall(arguments);
        };

        return call != null ? new ShortcutParameterValue(raw, prefix, call, suffix) : null;
    }

    private List<String> parseArguments(String arguments)
    {
        List<String> result = new ArrayList<>();

        Matcher matcher = ARGUMENT_PATTERN.matcher(arguments);
        boolean expectingArgument = true;
        while (expectingArgument && matcher.find()) {
            // One of the two alternatives (single or double quoted) always matches.
            result.add(Objects.requireNonNullElse(matcher.group(1), matcher.group(2)));
            expectingArgument = COMMA.equals(matcher.group(3));
        }

        // Fail if there was a trailing comma or text that isn't an argument.
        return expectingArgument ? null : result;
    }

    private Supplier<String> getLocalizationCall(List<String> arguments)
    {
        if (arguments.size() != 1) {
            return null;
        }

        String key = arguments.get(0);

        // Same as LocalizationScriptService#render(String), which returns the key when there is no translation.
        return () -> Objects.requireNonNullElse(this.localizationManagerProvider.get().getTranslationPlain(key), key);
    }

    private Supplier<String> getWebJarCall(List<String> arguments)
    {
        // Same as the WebJarsScriptService#url methods with string parameters.
        return switch (arguments.size()) {
            case 1 -> () -> this.webJarsUrlFactoryProvider.get().url(arguments.get(0));
            case 2 -> () -> this.webJarsUrlFactoryProvider.get().url(arguments.get(0), arguments.get(1));
            case 3 -> () -> this.webJarsUrlFactoryProvider.get().url(arguments.get(0), arguments.get(1),
                arguments.get(2));
            default -> null;
        };
    }

    private Supplier<String> getSkinFileCall(List<String> arguments)
    {
        if (arguments.size() != 1) {
            return null;
        }

        String fileName = arguments.get(0);

        // Same as com.xpn.xwiki.api.XWiki#getSkinFile(String).
        return () -> {
            XWikiContext xcontext = this.xcontextProvider.get();
            return xcontext.getWiki().getSkinFile(fileName, xcontext);
        };
    }
}
