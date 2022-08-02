package org.xwiki.officeimporter.document;

/**
 * Adds a backward compatibility layer on {@link OfficeDocument}.
 *
 * @version $Id$
 * @since 13.1RC1
 */
public privileged aspect OfficeDocumentCompatibilityAspect
{
    declare parents : OfficeDocument implements CompatibilityOfficeDocument;
}
