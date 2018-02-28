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
package org.xwiki.rendering.internal.renderer;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.configuration.RenderingConfiguration;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.renderer.reference.link.LinkLabelGenerator;

/**
 * Resolve configuration link format using the defined Rendering Configuration
 * (see {@link org.xwiki.rendering.configuration.RenderingConfiguration}).
 *
 * @version $Id$
 * @since 2.0M1
 */
@Component
@Singleton
public class XWikiLinkLabelGenerator implements LinkLabelGenerator
{
    @Inject
    private RenderingConfiguration renderingConfiguration;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private EntityReferenceResolver<ResourceReference> resourceReferenceResolver;

    @Inject
    private EntityReferenceProvider defaultEntityReferenceProvider;

    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> localReferenceSerializer;

    /**
     * {@inheritDoc}
     * 
     * @since 2.5RC1
     */
    @Override
    public String generate(ResourceReference reference)
    {
        StringBuilder result = new StringBuilder();

        String format = this.renderingConfiguration.getLinkLabelFormat();

        EntityReference resolvedReference = resourceReferenceResolver.resolve(reference, EntityType.DOCUMENT);
        if (resolvedReference == null) {
            throw new IllegalArgumentException(String.valueOf(reference));
        }
        DocumentReference documentReference = new DocumentReference(resolvedReference);

        for (int i = 0; i < format.length(); i++) {
            char c = format.charAt(i);
            if (c == '%' && i + 1 < format.length()) {
                // Check first letter after '%'
                i++;
                char cc = format.charAt(i);
                switch (cc) {
                    case 's':
                        // Replace %s with the full space name (e.g. space1.space2)
                        result.append(getSpacesLabel(documentReference));
                        break;
                    case 'p':
                        // Replace %p with the page name
                        result.append(getPageLabel(documentReference));
                        break;
                    case 't':
                        // Replace %t with the document title and fall back to %np if the title is null or empty
                        result.append(getTitleLabel(documentReference));
                        break;
                    case 'P':
                        // Replace %P with the page name in camel case + space
                        result.append(getCamelPageLabel(documentReference));
                        break;
                    case 'w':
                        // Replace %w with the wiki name
                        result.append(getWikiLabel(documentReference));
                        break;
                    case 'l':
                        if (i + 1 < format.length()) {
                            i++;
                            char ccc = format.charAt(i);
                            if (ccc == 's') {
                                // Replace %ls with the last space name
                                result.append(getLastSpaceLabel(documentReference));
                            } else {
                                result.append(c);
                                result.append(cc);
                                result.append(ccc);
                            }
                        } else {
                            result.append(c);
                            result.append(cc);
                        }
                        break;
                    case 'n':
                        if (i + 1 < format.length()) {
                            i++;
                            char ccc = format.charAt(i);
                            if (ccc == 'p') {
                                // Replace %np with the page name if the name is not the default page name
                                // (e.g. "WebHome") or with the last space name if it is.
                                result.append(getNestedPageLabel(documentReference));
                            } else {
                                result.append(c);
                                result.append(cc);
                                result.append(ccc);
                            }
                        } else {
                            result.append(c);
                            result.append(cc);
                        }
                        break;
                    case 'N':
                        if (i + 1 < format.length()) {
                            i++;
                            char ccc = format.charAt(i);
                            if (ccc == 'P') {
                                // Replace %NP with the nested page name in camel case + space
                                result.append(getCamelNestedPageLabel(documentReference));
                            } else {
                                result.append(c);
                                result.append(cc);
                                result.append(ccc);
                            }
                        } else {
                            result.append(c);
                            result.append(cc);
                        }
                        break;
                    default:
                        result.append(c);
                        result.append(cc);
                }
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    private String getCamelNestedPageLabel(DocumentReference documentReference)
    {
        return convertCamelString(getNestedPageLabel(documentReference));
    }

    private String getLastSpaceLabel(DocumentReference documentReference)
    {
        return documentReference.getLastSpaceReference().getName();
    }

    private String getWikiLabel(DocumentReference documentReference)
    {
        return documentReference.getWikiReference().getName();
    }

    private String getCamelPageLabel(DocumentReference documentReference)
    {
        return convertCamelString(documentReference.getName());
    }

    private String convertCamelString(String value)
    {
        return value.replaceAll("([a-z])([A-Z])", "$1 $2");
    }

    private String getPageLabel(DocumentReference documentReference)
    {
        return documentReference.getName();
    }

    private String getSpacesLabel(DocumentReference documentReference)
    {
        return this.localReferenceSerializer.serialize(documentReference.getParent());
    }

    private String getNestedPageLabel(DocumentReference documentReference)
    {
        String result;
        if (this.defaultEntityReferenceProvider.getDefaultReference(EntityType.DOCUMENT).getName().equals(
            documentReference.getName()))
        {
            result = documentReference.getLastSpaceReference().getName();
        } else {
            result = documentReference.getName();
        }
        return result;
    }

    private String getTitleLabel(DocumentReference documentReference)
    {
        String result;
        try {
            DocumentModelBridge document = this.documentAccessBridge.getTranslatedDocumentInstance(documentReference);
            if (StringUtils.isNotBlank(document.getTitle())) {
                result = document.getTitle();
            } else {
                // Title is empty, fall back to %np
                result = getNestedPageLabel(documentReference);
            }
        } catch (Exception e) {
            // If there's an error (meaning the document cannot be retrieved from the database for some reason)
            // the fall back to displaying %np
            result = getNestedPageLabel(documentReference);
        }
        return result;
    }
}
