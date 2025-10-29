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
package org.xwiki.flamingo.test.docker;

import org.junit.jupiter.api.Nested;
import org.xwiki.test.docker.junit5.UITest;

/**
 * All UI tests for the Flamingo Skin. Note that XWiki is started/stopped only once during all the tests and thus they
 * must all work as a single ordered scenario.
 *
 * @version $Id$
 * @since 11.2RC1
 */
@UITest
public class AllIT
{
    @Nested
    class NestedEditIT extends EditIT
    {
    }

    @Nested
    class NestedEditTranslationIT extends EditTranslationIT
    {
    }

    @Nested
    class NestedAttachmentIT extends AttachmentIT
    {
    }

    @Nested
    class NestedVelocityIT extends VelocityIT
    {
    }

    @Nested
    class NestedLoginIT extends LoginIT
    {
    }

    @Nested
    class NestedLoginProtectionIT extends LoginProtectionIT
    {
    }

    @Nested
    class NestedWikiMacroIT extends WikiMacroIT
    {
    }

    @Nested
    class NestedNavigationIT extends NavigationIT
    {
    }

    @Nested
    class NestedDeletePageIT extends DeletePageIT
    {
    }

    @Nested
    class NestedObjectEditorIT extends ObjectEditorIT
    {
    }

    @Nested
    class NestedRenamePageIT extends RenamePageIT
    {
    }

    @Nested
    class NestedCopyPageIT extends CopyPageIT
    {
    }

    @Nested
    class NestedSectionEditIT extends SectionEditIT
    {
    }
    
    @Nested
    class NestedInformationIT extends InformationIT
    {
    }

    @Nested
    class NestedViewersIT extends ViewersIT
    {
    }

    @Nested
    class NestedVersionIT extends VersionIT
    {
    }
    
    @Nested
    class NestedEditClassIT extends EditClassIT
    {
    }

    @Nested
    class NestedBeanValidationIT extends BeanValidationIT
    {
    }

    @Nested
    class NestedCommentsIT extends CommentsIT
    {
    }

    @Nested
    class NestedCreatePageAndSpaceIT extends CreatePageAndSpaceIT
    {
    }

    @Nested
    class NestedRecycleBinIT extends RecycleBinIT
    {
    }

    @Nested
    class NestedPDFExportIT extends PDFExportIT
    {
    }
    
    @Nested
    class NestedBacklinksIT extends BacklinksIT
    {
    }

    @Nested
    class NestedTextAreaIT extends TextAreaIT
    {
    }

    @Nested
    class NestedSheetIT extends SheetIT
    {
    }

    @Nested
    class NestedScriptAuthorIT extends ScriptAuthorIT
    {
    }

    @Nested
    class NestedFormTokenInjectionIT extends FormTokenInjectionIT
    {
    }

    @Nested
    class NestedPagePickerIT extends PagePickerIT
    {
    }

    @Nested
    class NestedCompareIT extends CompareIT
    {
    }

    @Nested
    class NestedXARExportIT extends XARExportIT
    {
    }

    @Nested
    class NestedPageReadyIT extends PageReadyIT
    {
    }

    @Nested
    class NestedSecurityCacheStressIT extends SecurityCacheStressIT
    {
    }

    @Nested
    class NestedServletEnvironmentCacheIT extends ServletEnvironmentCacheIT
    {
    }

    @Nested
    class NestedDocExtraTabsIT extends DocExtraTabsIT
    {
    }
}
