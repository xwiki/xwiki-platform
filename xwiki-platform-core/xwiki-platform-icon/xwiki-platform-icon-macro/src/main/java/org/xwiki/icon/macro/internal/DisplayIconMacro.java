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
package org.xwiki.icon.macro.internal;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.bridge.internal.DocumentContextExecutor;
import org.xwiki.component.annotation.Component;
import org.xwiki.icon.IconException;
import org.xwiki.icon.IconRenderer;
import org.xwiki.icon.IconSet;
import org.xwiki.icon.IconSetManager;
import org.xwiki.icon.macro.DisplayIconMacroParameters;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.async.internal.AbstractExecutedContentMacro;
import org.xwiki.rendering.async.internal.block.BlockAsyncRendererConfiguration;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.SpaceBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.ClassBlockMatcher;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.user.UserReferenceSerializer;

/**
 * Macro for displaying an icon.
 *
 * @version $Id$
 * @since 14.10.6
 * @since 15.2RC1
 */
@Component
@Named("displayIcon")
@Singleton
public class DisplayIconMacro extends AbstractExecutedContentMacro<DisplayIconMacroParameters>
{
    private static final String DESCRIPTION = "Display an icon.";

    @Inject
    private IconSetManager iconSetManager;

    @Inject
    private IconRenderer iconRenderer;

    @Inject
    private EntityReferenceSerializer<String> defaultEntityReferenceSerializer;

    @Inject
    private ContextualAuthorizationManager contextualAuthorization;

    @Inject
    @Named("document")
    private UserReferenceSerializer<DocumentReference> documentUserSerializer;

    @Inject
    private DocumentContextExecutor documentContextExecutor;

    /**
     * Default constructor.
     */
    public DisplayIconMacro()
    {
        super("Icon", DESCRIPTION, null, DisplayIconMacroParameters.class);

        setDefaultCategories(Set.of(DEFAULT_CATEGORY_CONTENT));
    }

    @Override
    public List<Block> execute(DisplayIconMacroParameters parameters, String content,
        MacroTransformationContext context) throws MacroExecutionException
    {
        List<Block> result;

        try {
            IconSet iconSet = getIconSet(parameters);

            if (iconSet == null) {
                result = List.of();
            } else {
                XDOM iconBlock = parseIcon(parameters, context, iconSet);

                BlockAsyncRendererConfiguration rendererConfiguration =
                    createBlockAsyncRendererConfiguration(null, iconBlock, null, context);
                rendererConfiguration.setAsyncAllowed(false);
                rendererConfiguration.setCacheAllowed(false);

                if (iconSet.getSourceDocumentReference() != null) {
                    DocumentReference sourceDocumentReference = iconSet.getSourceDocumentReference();

                    DocumentModelBridge sourceDocument =
                        this.documentAccessBridge.getDocumentInstance(sourceDocumentReference);
                    DocumentReference authorReference =
                        this.documentUserSerializer.serialize(sourceDocument.getAuthors().getContentAuthor());

                    rendererConfiguration.setSecureReference(sourceDocumentReference, authorReference);
                    rendererConfiguration.useEntity(sourceDocumentReference);

                    String stringDocumentReference =
                        this.defaultEntityReferenceSerializer.serialize(iconSet.getSourceDocumentReference());
                    rendererConfiguration.setTransformationId(stringDocumentReference);
                    rendererConfiguration.setResricted(false);

                    result = this.documentContextExecutor.call(
                        () -> List.of(this.executor.execute(rendererConfiguration)),
                        sourceDocument
                    );
                } else {
                    result = List.of(this.executor.execute(rendererConfiguration));
                }
            }
        } catch (MacroExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new MacroExecutionException("Failed parsing and executing the icon.", e);
        }

        return result;
    }

    private XDOM parseIcon(DisplayIconMacroParameters parameters, MacroTransformationContext context, IconSet iconSet)
        throws IconException, MacroExecutionException
    {
        String iconContent = this.iconRenderer.render(parameters.getName(), iconSet);
        // Force the icon to be more than, e.g., just a raw HTML macro to ensure that we get a wrapping paragraph
        // around it when the icon isn't inline.
        // This also forces the icon itself to be inline.
        if (StringUtils.isNotBlank(iconContent)) {
            iconContent = " " + iconContent;
        }
        MetaData metaData = null;

        if (iconSet.getSourceDocumentReference() != null) {
            String stringReference =
                this.defaultEntityReferenceSerializer.serialize(iconSet.getSourceDocumentReference());
            metaData = new MetaData(Map.of(MetaData.SOURCE, stringReference));
        }

        XDOM result = this.parser.parse(iconContent, Syntax.XWIKI_2_1, context, false, metaData,
            context.isInline());

        // Remove the space we inserted again.
        Block firstSpace = result.getFirstBlock(new ClassBlockMatcher(SpaceBlock.class), Block.Axes.DESCENDANT);
        if (firstSpace != null) {
            firstSpace.getParent().removeBlock(firstSpace);
        }
        return result;
    }

    private IconSet getIconSet(DisplayIconMacroParameters parameters) throws IconException, MacroExecutionException
    {
        IconSet iconSet;

        if (parameters.getIconSet() == null) {
            iconSet = this.iconSetManager.getCurrentIconSet();
        } else {
            iconSet = this.iconSetManager.getIconSet(parameters.getIconSet());
        }

        // Check if the current user can access the icon theme. If not, fall back to the default icon theme or throw
        // an exception when the fallback is disabled.
        if (iconSet != null && iconSet.getSourceDocumentReference() != null
            && !this.contextualAuthorization.hasAccess(Right.VIEW, iconSet.getSourceDocumentReference()))
        {
            if (parameters.isFallback()) {
                iconSet = null;
            } else {
                throw new MacroExecutionException(
                    String.format("Current user [%s] doesn't have view rights on the icon set's document [%s]",
                        this.documentAccessBridge.getCurrentUserReference(), iconSet.getSourceDocumentReference()));
            }
        }

        if (parameters.isFallback() && (iconSet == null || !iconSet.hasIcon(parameters.getName()))) {
            iconSet = this.iconSetManager.getDefaultIconSet();
        }

        return iconSet;
    }
}
