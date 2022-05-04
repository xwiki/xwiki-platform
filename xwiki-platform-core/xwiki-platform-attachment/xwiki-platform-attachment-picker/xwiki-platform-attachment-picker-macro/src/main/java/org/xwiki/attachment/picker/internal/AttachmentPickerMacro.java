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
package org.xwiki.attachment.picker.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.attachment.picker.AttachmentPickerMacroParameters;
import org.xwiki.component.annotation.Component;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.skinx.SkinExtension;

import static java.util.Map.entry;

/**
 * Display an attachment picker with a search field and a grid preview of the attachments. This widget is adapted to
 * UIs where vertical space usage is not an issue.
 *
 * @version $Id$
 * @since 14.4RC1
 */
@Component
@Named("attachmentPicker")
@Singleton
public class AttachmentPickerMacro extends AbstractMacro<AttachmentPickerMacroParameters>
{
    /**
     * id parameter key for the {@link Block}s returned by
     * {@link #execute(AttachmentPickerMacroParameters, String, MacroTransformationContext)}.
     */
    private static final String BLOCK_PARAM_ID = "id";

    /**
     * class parameter key for the {@link Block}s returned by
     * {@link #execute(AttachmentPickerMacroParameters, String, MacroTransformationContext)}.
     */
    private static final String BLOCK_PARAM_CLASS = "class";

    private static final String ATTACHMENT_PICKER_CLASSES = "attachmentPicker";

    /**
     * CSS file skin extension, to include the attachment picker css.
     */
    @Inject
    @Named("ssfx")
    private SkinExtension ssfx;

    /**
     * JS file skin extension, to include the attachment picker javascript. A single javascript file provided by the war
     * module is required. This file has some velocity to define the other webjar dependencies dynamically, all the rest
     * is provided by the webjar module and its dependencies.
     */
    @Inject
    @Named("jsfx")
    private SkinExtension jsfx;

    @Inject
    private ContextualLocalizationManager l10n;

    /**
     * Default constructor.
     */
    public AttachmentPickerMacro()
    {
        super("Attachment Picker", "Grid based attachment picker.", AttachmentPickerMacroParameters.class);
    }

    @Override
    public boolean supportsInlineMode()
    {
        return false;
    }

    @Override
    public List<Block> execute(AttachmentPickerMacroParameters parameters, String content,
        MacroTransformationContext context)
    {
        Map<String, Object> forceSkinAction = Map.of("forceSkinAction", true);
        this.jsfx.use("uicomponents/widgets/attachmentPicker.js", forceSkinAction);
        this.ssfx.use("uicomponents/widgets/attachmentPicker.css", forceSkinAction);
        Map<String, String> attachmentPickerParameters = new HashMap<>(Map.ofEntries(
            entry(BLOCK_PARAM_ID, parameters.getId()),
            entry(BLOCK_PARAM_CLASS, ATTACHMENT_PICKER_CLASSES),
            entry("data-xwiki-lightbox", "false"),
            entry("data-xwiki-attachment-picker-filter", String.join(",", parameters.getFilter()))
        ));
        if (parameters.getLimit() != null) {
            attachmentPickerParameters.put("data-xwiki-attachment-picker-limit", String.valueOf(parameters.getLimit()));
        }

        return List.of(new GroupBlock(List.of(
            // Search block.
            new GroupBlock(List.of(), Map.of(BLOCK_PARAM_CLASS, "attachmentPickerSearch")),
            // Results block.
            new GroupBlock(Map.of(BLOCK_PARAM_CLASS, "attachmentPickerResults")),
            // No results block.
            new GroupBlock(List.of(this.l10n.getTranslation("attachment.picker.macro.notResult.message").render()),
                Map.of(BLOCK_PARAM_CLASS, "attachmentPickerNoResults hidden box warningmessage"))
        ), attachmentPickerParameters));
    }
}
