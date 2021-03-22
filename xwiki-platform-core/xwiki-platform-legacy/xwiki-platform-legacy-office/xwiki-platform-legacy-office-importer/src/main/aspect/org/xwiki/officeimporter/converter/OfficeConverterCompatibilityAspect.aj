package org.xwiki.officeimporter.converter;

/**
 * Add a backward compatibility layer to {@link OfficeConverter}.
 *
 * @version $Id$
 * @since 13.1RC1
 */
public privileged aspect OfficeConverterCompatibilityAspect
{
    declare parents : OfficeConverter implements CompatibilityOfficeConverter;
}
