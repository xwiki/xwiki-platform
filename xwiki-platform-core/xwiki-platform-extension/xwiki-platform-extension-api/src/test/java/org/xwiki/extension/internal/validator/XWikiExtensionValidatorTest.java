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
package org.xwiki.extension.internal.validator;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.component.namespace.Namespace;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstallException;
import org.xwiki.extension.test.EmptyExtension;
import org.xwiki.job.DefaultRequest;
import org.xwiki.model.EntityType;
import org.xwiki.model.namespace.UserNamespace;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/**
 * Validate {@link XWikiExtensionValidator}.
 * 
 * @version $Id$
 */
@ComponentTest
public class XWikiExtensionValidatorTest
{
    private static final DocumentReference USER_REFERENCE = new DocumentReference("wiki", "XWiki", "user");

    private static final String USER_REFERENCE_STRING = "wiki:XWiki.user";

    private static final Namespace USER_REFERENCE_NAMESPACE = new UserNamespace(USER_REFERENCE_STRING);

    private static final DocumentReference CALLER_REFERENCE = new DocumentReference("wiki", "XWiki", "caller");

    @MockComponent
    private AuthorizationManager authorization;

    @MockComponent
    @Named("relative")
    protected EntityReferenceResolver<String> resolver;

    @InjectMockComponents
    private XWikiExtensionValidator validator;

    private DefaultRequest request = new DefaultRequest();

    private Extension extension = new EmptyExtension(new ExtensionId("id", "version"), "type");

    private void assertCheckInstallPass() throws InstallException
    {
        assertCheckInstallPass(Namespace.ROOT);
    }

    private void assertCheckInstallPass(Namespace namespace) throws InstallException
    {
        this.validator.checkInstall(this.extension, namespace.serialize(), this.request);
    }

    private void assertCheckInstallFail()
    {
        assertCheckInstallFail(Namespace.ROOT);
    }

    private void assertCheckInstallFail(Namespace namespace)
    {
        assertThrows(InstallException.class, () -> {
            assertCheckInstallPass(namespace);
        });
    }

    @BeforeEach
    public void beforeEach() throws AccessDeniedException
    {
        when(this.resolver.resolve(USER_REFERENCE_STRING, EntityType.DOCUMENT)).thenReturn(USER_REFERENCE);

        doThrow(AccessDeniedException.class).when(this.authorization).checkAccess(any(), any(), any());
    }

    // Tests

    @Test
    public void checkRightDisabled() throws InstallException
    {
        assertCheckInstallPass();

        this.request.setProperty(AbstractExtensionValidator.PROPERTY_CHECKRIGHTS, false);

        assertCheckInstallPass();

        this.request.setProperty(AbstractExtensionValidator.PROPERTY_USERREFERENCE, USER_REFERENCE);
        this.request.setProperty(AbstractExtensionValidator.PROPERTY_CALLERREFERENCE, CALLER_REFERENCE);

        assertCheckInstallPass();

        this.request.setProperty(AbstractExtensionValidator.PROPERTY_CHECKRIGHTS_USER, true);
        this.request.setProperty(AbstractExtensionValidator.PROPERTY_CHECKRIGHTS_CALLER, true);

        assertCheckInstallPass();
    }

    @Test
    public void checkRight() throws AccessDeniedException, InstallException
    {
        this.request.setProperty(AbstractExtensionValidator.PROPERTY_CHECKRIGHTS, true);
        this.request.setProperty(AbstractExtensionValidator.PROPERTY_CHECKRIGHTS_USER, true);
        this.request.setProperty(AbstractExtensionValidator.PROPERTY_CHECKRIGHTS_CALLER, true);

        assertCheckInstallFail();

        this.request.setProperty(AbstractExtensionValidator.PROPERTY_USERREFERENCE, USER_REFERENCE);
        this.request.setProperty(AbstractExtensionValidator.PROPERTY_CALLERREFERENCE, CALLER_REFERENCE);

        assertCheckInstallFail();

        doNothing().when(this.authorization).checkAccess(Right.PROGRAM, USER_REFERENCE, null);

        assertCheckInstallFail();

        doNothing().when(this.authorization).checkAccess(Right.PROGRAM, CALLER_REFERENCE, null);

        assertCheckInstallPass();
    }

    @Test
    public void checkRightOnUserNamespace() throws InstallException, AccessDeniedException
    {
        this.request.setProperty(AbstractExtensionValidator.PROPERTY_CHECKRIGHTS, true);
        this.request.setProperty(AbstractExtensionValidator.PROPERTY_CHECKRIGHTS_USER, true);
        this.request.setProperty(AbstractExtensionValidator.PROPERTY_CHECKRIGHTS_CALLER, true);

        assertCheckInstallFail();

        assertCheckInstallFail(USER_REFERENCE_NAMESPACE);

        this.request.setProperty(AbstractExtensionValidator.PROPERTY_USERREFERENCE, USER_REFERENCE);

        assertCheckInstallFail(USER_REFERENCE_NAMESPACE);

        this.request.setProperty(AbstractExtensionValidator.PROPERTY_CALLERREFERENCE, USER_REFERENCE);

        assertCheckInstallPass(USER_REFERENCE_NAMESPACE);

        this.request.setProperty(AbstractExtensionValidator.PROPERTY_CALLERREFERENCE, CALLER_REFERENCE);

        assertCheckInstallFail(USER_REFERENCE_NAMESPACE);

        doNothing().when(this.authorization).checkAccess(Right.PROGRAM, CALLER_REFERENCE, null);

        assertCheckInstallPass(USER_REFERENCE_NAMESPACE);
    }
}
