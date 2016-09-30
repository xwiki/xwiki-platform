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
package com.xpn.xwiki.plugin.scheduler.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.sheet.SheetBinder;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.mandatory.AbstractMandatoryDocumentInitializer;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;

/**
 * Update the XWiki.SchedulerJobClass document with all required information.
 * 
 * @version $Id$
 * @since 6.1M1
 */
@Component
@Named("XWiki.SchedulerJobSheet")
@Singleton
public class SchedulerJobClassDocumentInitializer extends AbstractMandatoryDocumentInitializer
{
    /**
     * The name of the sheet used to display instances of the class.
     */
    public static final String SHEET_NAME = "SchedulerJobSheet";

    /**
     * Local reference of the XWiki Scheduler Job Class representing a job that can be scheduled by this plugin.
     */
    public static final LocalDocumentReference XWIKI_JOB_CLASSREFERENCE = new LocalDocumentReference(
        XWiki.SYSTEM_SPACE, "SchedulerJobClass");

    private static final String FIELD_JOBNAME = "jobName";

    private static final String FIELD_JOBDESCRIPTION = "jobDescription";

    private static final String FIELD_JOBCLASS = "jobClass";

    private static final String FIELD_STATUS = "status";

    private static final String FIELD_CRON = "cron";

    private static final String FIELD_SCRIPT = "script";

    private static final String FIELD_CONTEXTUSER = "contextUser";

    private static final String FIELD_CONTEXTLANG = "contextLang";

    private static final String FIELD_CONTEXTDATABASE = "contextDatabase";

    /**
     * Used to bind a class to a document sheet.
     */
    @Inject
    @Named("class")
    private SheetBinder classSheetBinder;

    /**
     * Default constructor.
     */
    public SchedulerJobClassDocumentInitializer()
    {
        // Since we can`t get the main wiki here, this is just to be able to use the Abstract class.
        // getDocumentReference() returns the actual main wiki document reference.
        super(XWiki.SYSTEM_SPACE, XWIKI_JOB_CLASSREFERENCE.getName());
    }

    @Override
    public boolean updateDocument(XWikiDocument document)
    {
        boolean needsUpdate = false;

        // Add missing class fields
        BaseClass baseClass = document.getXClass();

        needsUpdate |= baseClass.addTextField(FIELD_JOBNAME, "Job Name", 60);
        needsUpdate |= baseClass.addTextAreaField(FIELD_JOBDESCRIPTION, "Job Description", 45, 10);
        needsUpdate |= baseClass.addTextField(FIELD_JOBCLASS, "Job Class", 60);
        needsUpdate |= baseClass.addTextField(FIELD_STATUS, "Status", 30);
        needsUpdate |= baseClass.addTextField(FIELD_CRON, "Cron Expression", 30);

        // This field contains groovy script and is thus of tpye PureText.
        // TODO: In the future, add the ability to provide wiki markup so that all script languages can be supported
        // and not only Groovy. When this is done, convert this field to "Text".
        needsUpdate |= baseClass.addTextAreaField(FIELD_SCRIPT, "Job Script", 60, 10,
            TextAreaClass.ContentType.PURE_TEXT);
        needsUpdate |= baseClass.addTextField(FIELD_CONTEXTUSER, "Job execution context user", 30);
        needsUpdate |= baseClass.addTextField(FIELD_CONTEXTLANG, "Job execution context lang", 30);
        needsUpdate |= baseClass.addTextField(FIELD_CONTEXTDATABASE, "Job execution context database", 30);

        // Add missing document fields
        needsUpdate |= setClassDocumentFields(document, "XWiki Scheduler Job Class");

        // Use SchedulerJobSheet to display documents having SchedulerJobClass objects if no other class sheet is
        // specified.
        if (this.classSheetBinder.getSheets(document).isEmpty()) {
            String wikiName = document.getDocumentReference().getWikiReference().getName();
            DocumentReference sheet = new DocumentReference(wikiName, XWiki.SYSTEM_SPACE, SHEET_NAME);
            needsUpdate |= this.classSheetBinder.bind(document, sheet);
        }

        return needsUpdate;
    }
}
