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
package org.xwiki.job.handler.internal.question;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.script.ScriptContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.container.Request;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.csrf.CSRFToken;
import org.xwiki.job.Job;
import org.xwiki.job.JobExecutor;
import org.xwiki.job.event.status.CancelableJobStatus;
import org.xwiki.job.handler.internal.AbstractTemplateJobResourceReferenceHandler;
import org.xwiki.properties.BeanDescriptor;
import org.xwiki.properties.BeanManager;
import org.xwiki.properties.PropertyDescriptor;
import org.xwiki.properties.PropertyException;
import org.xwiki.resource.ResourceReferenceHandlerException;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.url.internal.ParentResourceReference;

/**
 * Execute the right template depending on the job question.
 * 
 * @version $Id$
 */
@Component
@Named("question")
@Singleton
public class QuestionJobResourceReferenceHandler extends AbstractTemplateJobResourceReferenceHandler
{
    private static final String VM = ".vm";

    private static final String QPROPERTY_PREFIX = "qproperty_";

    private static final String QPROPERTY_ARRAY_SUFFIX = "[]";

    private static final String CANCEL = "cancel";

    @Inject
    private JobExecutor executor;

    @Inject
    private ScriptContextManager scriptContextManager;

    @Inject
    private BeanManager beans;

    @Inject
    private CSRFToken csrf;

    private String[] getTemplates(Class<?> questionClass, String jobType, String prefix)
    {
        String className = questionClass.getName();
        String classSimpleName = questionClass.getSimpleName();

        String[] templates = new String[6];

        templates[0] = prefix + jobType + '/' + className + VM;
        templates[1] = prefix + jobType + '/' + classSimpleName + VM;
        templates[2] = prefix + jobType + VM;
        templates[3] = prefix + className + VM;
        templates[4] = prefix + classSimpleName + VM;
        templates[5] = prefix + "default" + VM;

        return templates;
    }

    @Override
    public void handle(ParentResourceReference reference) throws ResourceReferenceHandlerException
    {
        List<String> jobId = reference.getPathSegments();

        Job job = this.executor.getJob(jobId);
        if (job == null) {
            throw new ResourceReferenceHandlerException("Cannot find any running job with id " + jobId);
        }

        Object question = job.getStatus().getQuestion();
        if (question == null) {
            throw new ResourceReferenceHandlerException("The job with id " + jobId + " does not have any question");
        }

        Request request = this.container.getRequest();

        String prefix = "question/";
        String contentType = "text/html; charset=utf-8";

        // POST request means answer
        if (request instanceof ServletRequest) {
            HttpServletRequest httpRequest = ((ServletRequest) request).getHttpServletRequest();

            if (httpRequest.getMethod().equals("POST")) {
                String token = httpRequest.getParameter("form_token");

                // TODO: should probably move this check in some filter triggered by an annotation
                if (this.csrf.isTokenValid(token)) {
                    answer(httpRequest, job, jobId, question);

                    prefix = "answer/";
                    contentType = "application/json";
                } else {
                    // TODO: Throw some exception
                }
            }
        }

        String jobType = job.getType();

        // Provide informations about the job to the template
        this.scriptContextManager.getCurrentScriptContext().setAttribute("job", job, ScriptContext.ENGINE_SCOPE);

        String[] templates = getTemplates(question.getClass(), jobType, prefix);
        if (!tryTemplates(contentType, templates)) {
            throw new ResourceReferenceHandlerException(
                "Cannot find any template for the job with id" + jobId + " (" + Arrays.toString(templates) + ")");
        }
    }

    private boolean isIterable(PropertyDescriptor propertyDescriptor)
    {
        Type type = propertyDescriptor.getPropertyType();

        if (TypeUtils.isArrayType(type)) {
            return true;
        }

        return TypeUtils.isAssignable(type, Iterable.class);
    }

    private void answer(HttpServletRequest request, Job job, List<String> jobId, Object question)
        throws ResourceReferenceHandlerException
    {
        // Populate the question

        try {
            this.beans.populate(question, extractParameters(request, question));
        } catch (PropertyException e) {
            throw new ResourceReferenceHandlerException(
                String.format("Failed to populate question object for job with id [%s]", jobId), e);
        }

        // Cancel if supported
        if (job.getStatus() instanceof CancelableJobStatus
            && Boolean.parseBoolean(request.getParameter(CANCEL))) {
            ((CancelableJobStatus) job.getStatus()).cancel();
        }

        // Answer question
        job.getStatus().answered();
    }

    private Map<String, Object> extractParameters(HttpServletRequest request, Object question)
    {
        Map<String, Object> parameters = new HashMap<>();

        BeanDescriptor descriptor = this.beans.getBeanDescriptor(question.getClass());

        for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
            if (entry.getKey().startsWith(QPROPERTY_PREFIX) && entry.getValue() != null
                && entry.getValue().length > 0) {
                String parameterKey = entry.getKey().substring(QPROPERTY_PREFIX.length());
                if (parameterKey.endsWith(QPROPERTY_ARRAY_SUFFIX)) {
                    parameterKey = parameterKey.substring(0, parameterKey.length() - QPROPERTY_ARRAY_SUFFIX.length());
                    parameters.put(parameterKey, entry.getValue());
                } else {
                    addValue(descriptor, parameterKey, parameters, entry.getValue());
                }
            }
        }

        return parameters;
    }

    private void addValue(BeanDescriptor descriptor, String parameterKey, Map<String, Object> parameters,
        String[] value)
    {
        PropertyDescriptor propertyDescriptor = descriptor.getProperty(parameterKey);
        if (propertyDescriptor != null) {
            if (isIterable(propertyDescriptor)) {
                parameters.put(parameterKey, value);
            } else {
                parameters.put(parameterKey, value[0]);
            }
        }
    }
}
