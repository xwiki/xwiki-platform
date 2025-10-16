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

import org.junit.jupiter.api.DisplayName;
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
    @DisplayName("Save Edit Comments Tests")
    class NestedEditIT extends EditIT
    {
    }

    @Nested
    @DisplayName("Edit Translation Tests")
    class NestedEditTranslationIT extends EditTranslationIT
    {
    }

    @Nested
    @DisplayName("Attachment Tests")
    class NestedAttachmentIT extends AttachmentIT
    {
    }

    @Nested
    @DisplayName("Velocity Macro Tests")
    class NestedVelocityIT extends VelocityIT
    {
    }

    @Nested
    @DisplayName("Login Tests")
    class NestedLoginIT extends LoginIT
    {
    }

    @Nested
    @DisplayName("Login Protection Tests")
    class NestedLoginProtectionIT extends LoginProtectionIT
    {
    }

    @Nested
    @DisplayName("WikiMacro Tests")
    class NestedWikiMacroIT extends WikiMacroIT
    {
    }

    @Nested
    @DisplayName("Navigation Tests")
    class NestedNavigationIT extends NavigationIT
    {
    }

    @Nested
    @DisplayName("Delete Page Tests")
    class NestedDeletePageIT extends DeletePageIT
    {
    }

    @Nested
    @DisplayName("Object editor Tests")
    class NestedObjectEditorIT extends ObjectEditorIT
    {
    }

    @Nested
    @DisplayName("Rename page tests")
    class NestedRenamePageIT extends RenamePageIT
    {
    }

    @Nested
    @DisplayName("Copy page tests")
    class NestedCopyPageIT extends CopyPageIT
    {
    }

    @Nested
    @DisplayName("Section editing tests")
    class NestedSectionEditIT extends SectionEditIT
    {
    }
    
    @Nested
    @DisplayName("Document information tab tests")
    class NestedInformationIT extends InformationIT
    {
    }

    @Nested
    @DisplayName("Page viewers tests")
    class NestedViewersIT extends ViewersIT
    {
    }

    @Nested
    @DisplayName("Page history manipulation tests")
    class NestedVersionIT extends VersionIT
    {
    }
    
    @Nested
    @DisplayName("Edit Class tests")
    class NestedEditClassIT extends EditClassIT
    {
    }

    @Nested
    @DisplayName("Bean Validation tests")
    class NestedBeanValidationIT extends BeanValidationIT
    {
    }

    @Nested
    @DisplayName("Comments tests")
    class NestedCommentsIT extends CommentsIT
    {
    }

    @Nested
    @DisplayName("Create Page And Space tests")
    class NestedCreatePageAndSpaceIT extends CreatePageAndSpaceIT
    {
    }

    @Nested
    @DisplayName("Restore deleted Page tests")
    class NestedRecycleBinIT extends RecycleBinIT
    {
    }

    @Nested
    @DisplayName("PDF Export Tests")
    class NestedPDFExportIT extends PDFExportIT
    {
    }
    
    @Nested
    @DisplayName("Backlinks Tests")
    class NestedBacklinksIT extends BacklinksIT
    {
    }

    @Nested
    @DisplayName("TextArea property Tests")
    class NestedTextAreaIT extends TextAreaIT
    {
    }

    @Nested
    @DisplayName("Sheet system Tests")
    class NestedSheetIT extends SheetIT
    {
    }

    @Nested
    @DisplayName("Script author Tests")
    class NestedScriptAuthorIT extends ScriptAuthorIT
    {
    }

    @Nested
    @DisplayName("Form Token injection Tests")
    class NestedFormTokenInjectionIT extends FormTokenInjectionIT
    {
    }

    @Nested
    @DisplayName("Page Picker Tests")
    class NestedPagePickerIT extends PagePickerIT
    {
    }

    @Nested
    @DisplayName("Compare Tests")
    class NestedCompareIT extends CompareIT
    {
    }

    @Nested
    @DisplayName("XAR Export Tests")
    class NestedXARExportIT extends XARExportIT
    {
    }

    @Nested
    @DisplayName("Page Ready Tests")
    class NestedPageReadyIT extends PageReadyIT
    {
    }

    @Nested
    @DisplayName("Security Cache Stress Tests")
    class NestedSecurityCacheStressIT extends SecurityCacheStressIT
    {
    }

    @Nested
    @DisplayName("Servlet Environment Cache Tests")
    class NestedServletEnvironmentCacheIT extends ServletEnvironmentCacheIT
    {
    }

    @Nested
    @DisplayName("Document Extra Tabs Tests")
    class NestedDocExtraTabsIT extends DocExtraTabsIT
    {
    }
}
