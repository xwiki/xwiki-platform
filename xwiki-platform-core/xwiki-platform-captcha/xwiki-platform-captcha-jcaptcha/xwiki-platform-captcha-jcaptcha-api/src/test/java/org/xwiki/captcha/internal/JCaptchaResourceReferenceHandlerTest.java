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
package org.xwiki.captcha.internal;

import java.io.IOException;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.captcha.CaptchaException;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.container.servlet.ServletResponse;
import org.xwiki.resource.ResourceReferenceHandlerChain;
import org.xwiki.resource.ResourceReferenceHandlerException;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.octo.captcha.service.CaptchaService;
import com.octo.captcha.service.image.ImageCaptchaService;
import com.octo.captcha.service.sound.SoundCaptchaService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Validate the behaviour of {@link JCaptchaResourceReferenceHandler}.
 *
 * @version $Id$
 * @since 11.10
 */
@ComponentTest
class JCaptchaResourceReferenceHandlerTest
{
    private static final String SESSION_ID = "customSessionId";

    private static final Locale LOCALE = new Locale("customLocale");

    @InjectMockComponents
    private JCaptchaResourceReferenceHandler jCaptchaResourceReferenceHandler;

    @MockComponent
    private CaptchaServiceManager captchaServiceManager;

    @Mock
    private HttpServletResponse response;

    @Mock
    private ServletOutputStream outputStream;

    @Mock
    private HttpSession httpSession;

    @Mock
    private ResourceReferenceHandlerChain resourceReferenceHandlerChain;

    @BeforeEach
    public void setup(MockitoComponentManager mockitoComponentManager) throws Exception
    {
        Container container = mockitoComponentManager.getInstance(Container.class);
        ServletRequest request = mock(ServletRequest.class);
        when(container.getRequest()).thenReturn(request);

        ServletResponse servletResponse = mock(ServletResponse.class);
        when(container.getResponse()).thenReturn(servletResponse);
        when(servletResponse.getHttpServletResponse()).thenReturn(this.response);
        when(this.response.getOutputStream()).thenReturn(this.outputStream);

        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        when(request.getHttpServletRequest()).thenReturn(httpServletRequest);
        when(httpServletRequest.getLocale()).thenReturn(LOCALE);

        when(httpServletRequest.getSession()).thenReturn(this.httpSession);
        when(this.httpSession.getId()).thenReturn(SESSION_ID);
    }

    @Test
    void handleCaptchaImage() throws ResourceReferenceHandlerException, CaptchaException, IOException
    {
        ImageCaptchaService imageCaptchaService = mock(ImageCaptchaService.class);
        when(this.captchaServiceManager.getCaptchaService("customImageEngine")).thenReturn(imageCaptchaService);

        JCaptchaResourceReference jCaptchaResourceReference =
            new JCaptchaResourceReference("image", "customImageEngine");
        this.jCaptchaResourceReferenceHandler.handle(jCaptchaResourceReference, this.resourceReferenceHandlerChain);

        verify(this.captchaServiceManager).getCaptchaService("customImageEngine");
        verify(this.httpSession).getId();
        verify(imageCaptchaService).getImageChallengeForID(SESSION_ID, LOCALE);
        verify(this.response).setContentType("image/jpeg");
        verify(this.response).getOutputStream();
        verify(this.outputStream).write(any());
        verify(this.resourceReferenceHandlerChain).handleNext(jCaptchaResourceReference);
    }

    @Test
    void handleCaptchaSound() throws ResourceReferenceHandlerException, CaptchaException, IOException
    {
        SoundCaptchaService soundCaptchaService = mock(SoundCaptchaService.class);
        when(this.captchaServiceManager.getCaptchaService("customSoundEngine")).thenReturn(soundCaptchaService);

        AudioInputStream audioInputStream = mock(AudioInputStream.class);
        when(soundCaptchaService.getSoundChallengeForID(SESSION_ID, LOCALE)).thenReturn(audioInputStream);
        AudioFormat audioFormat = mock(AudioFormat.class);
        // Specifying a random encoding format as we don't care what encoding is used.
        when(audioFormat.getEncoding()).thenReturn(AudioFormat.Encoding.PCM_SIGNED);
        when(audioInputStream.getFormat()).thenReturn(audioFormat);

        JCaptchaResourceReference jCaptchaResourceReference =
            new JCaptchaResourceReference("sound", "customSoundEngine");
        this.jCaptchaResourceReferenceHandler.handle(jCaptchaResourceReference, this.resourceReferenceHandlerChain);

        verify(this.captchaServiceManager).getCaptchaService("customSoundEngine");
        verify(this.httpSession).getId();
        verify(soundCaptchaService).getSoundChallengeForID(SESSION_ID, LOCALE);
        verify(this.response).setContentType("audio/x-wav");
        verify(this.response).getOutputStream();
        verify(this.outputStream).write(any());
        verify(this.resourceReferenceHandlerChain).handleNext(jCaptchaResourceReference);
    }

    @Test
    void handleCaptchaText() throws ResourceReferenceHandlerException, CaptchaException, IOException
    {
        CaptchaService textCaptchService = mock(CaptchaService.class);
        when(this.captchaServiceManager.getCaptchaService("customTextEngine")).thenReturn(textCaptchService);

        // just to have some data to write to check the mock.
        when(textCaptchService.getChallengeForID(SESSION_ID, LOCALE)).thenReturn("some data");

        JCaptchaResourceReference jCaptchaResourceReference =
            new JCaptchaResourceReference("text", "customTextEngine");
        this.jCaptchaResourceReferenceHandler.handle(jCaptchaResourceReference, this.resourceReferenceHandlerChain);

        verify(this.captchaServiceManager).getCaptchaService("customTextEngine");
        verify(this.httpSession).getId();
        verify(textCaptchService).getChallengeForID(SESSION_ID, LOCALE);
        verify(this.response).setContentType("text/plain");
        verify(this.response).getOutputStream();
        verify(this.outputStream).write(any(), anyInt(), anyInt());
        verify(this.outputStream).close();
        verify(this.resourceReferenceHandlerChain).handleNext(jCaptchaResourceReference);
    }

    @Test
    void handleWrongTypeCaptchaReference()
    {
        JCaptchaResourceReference jCaptchaResourceReference = new JCaptchaResourceReference("customType", "foobar");

        ResourceReferenceHandlerException resourceReferenceHandlerException =
            assertThrows(ResourceReferenceHandlerException.class, () -> {
                this.jCaptchaResourceReferenceHandler.handle(jCaptchaResourceReference, this.resourceReferenceHandlerChain);
            });

        assertEquals("Failed to handle resource [jcaptcha]", resourceReferenceHandlerException.getMessage());
        assertEquals(UnsupportedOperationException.class, resourceReferenceHandlerException.getCause().getClass());
        assertEquals("Unsupported value [customType] for the [type] parameter",
            resourceReferenceHandlerException.getCause().getMessage());
    }

    @Test
    void handleNullTypeCaptchaReference()
    {
        JCaptchaResourceReference jCaptchaResourceReference = new JCaptchaResourceReference(null, "foobar");

        ResourceReferenceHandlerException resourceReferenceHandlerException =
            assertThrows(ResourceReferenceHandlerException.class, () -> {
                this.jCaptchaResourceReferenceHandler.handle(jCaptchaResourceReference, this.resourceReferenceHandlerChain);
            });

        assertEquals("Failed to handle resource [jcaptcha]", resourceReferenceHandlerException.getMessage());
        assertEquals(IllegalArgumentException.class, resourceReferenceHandlerException.getCause().getClass());
        assertEquals("Missing [type] parameter", resourceReferenceHandlerException.getCause().getMessage());
    }

    @Test
    void handleErrorWhenGettingEngine() throws CaptchaException
    {
        JCaptchaResourceReference jCaptchaResourceReference = new JCaptchaResourceReference("text", "foobar");
        when(this.captchaServiceManager.getCaptchaService("foobar"))
            .thenThrow(new CaptchaException("Cannot found foobar service", new RuntimeException()));

        ResourceReferenceHandlerException resourceReferenceHandlerException =
            assertThrows(ResourceReferenceHandlerException.class, () -> {
                this.jCaptchaResourceReferenceHandler.handle(jCaptchaResourceReference, this.resourceReferenceHandlerChain);
            });

        assertEquals("Failed to handle resource [jcaptcha]", resourceReferenceHandlerException.getMessage());
        assertEquals(CaptchaException.class, resourceReferenceHandlerException.getCause().getClass());
        assertEquals("Cannot found foobar service", resourceReferenceHandlerException.getCause().getMessage());
    }
}
