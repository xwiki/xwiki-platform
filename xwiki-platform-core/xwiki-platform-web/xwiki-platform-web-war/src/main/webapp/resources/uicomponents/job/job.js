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
define('xwiki-job-messages', {
  prefix: 'job.question.notification.',
  keys: [
    'answering',
    'canceling'
  ]
});

require([
  'jquery',
  'xwiki-meta',
  'xwiki-job-runner',
  'xwiki-l10n!xwiki-job-messages'
], function($, xm, JobRunner, l10n) {
  var updateProgress = function(jobUI, job) {
    jobUI.find('.ui-progress-background').toggle(job.state !== 'NONE');
    jobUI.find('.ui-progress-message').toggle(job.state === 'NONE');

    if (job.state !== 'NONE') {
      jobUI.find('#state-none-hint').remove();
    } else {
      jobUI.find('#state-none-hint').removeClass("hidden");
    }

    var percent = Math.floor((job.progress.offset || 0) * 100);
    jobUI.find('.ui-progress-bar').css('width', percent + '%');
    var jobLog = job.log.items || [];
    if (jobLog.length) {
      jobUI.find('.ui-progress-message').html(jobLog[jobLog.length - 1].renderedMessage);
    }
  };

  var updateQuestion = function(jobUI, job, answerCallback) {
    var jobQuestion = jobUI.find('.ui-question');

    if (typeof answerCallback === 'function') {
      // Display the question
      var encodedJobId = job.id.map(encodeURIComponent).join('/');
      var displayerURL = XWiki.contextPath + '/job/wiki/' + xm.wiki + '/question/' + encodedJobId;

      // Remember the answer callback
      jobQuestion.data('answerCallback', answerCallback);

      $.get(displayerURL).then(updateQuestionContent.bind(jobQuestion));
    } else {
      jobQuestion.empty();
    }
  };

  var updateQuestionContent = function(data) {
    var jobQuestion = $(this);

    // Replace the question div content with the data from the request
    jobQuestion.html(data);

    // Indicate that a new question has been loaded
    jobQuestion.trigger('job:question:loaded');
  };

  var updateLog = function(jobUI, job) {
    var jobLog = job.log.items || [];
    var jobLogUI = jobUI.find('.log');
    if (job.log.offset === 0) {
      jobLogUI.html('');
    }
    jobLogUI.find('.log-item-loading').removeClass('log-item-loading');
    $.each(jobLog, function(index, item) {
      var classNames = ['log-item', 'log-item-' + item.level];
      if (job.state !== 'FINISHED' && index === jobLog.length - 1) {
        classNames.push('log-item-loading');
      }
      $(document.createElement('li')).addClass(classNames.join(' ')).html(item.renderedMessage).appendTo(jobLogUI);
    })
  };

  var resolveAnswerProperties = function(properties, questionForm) {
    if (typeof properties === 'function') {
      return properties.bind(questionForm)();
    }

    return properties;
  }

  var createAnswerProperties = function(questionForm, button) {
    // Create request parameters
    var properties = {};

    // Add form inputs (either data based on input elements based)
    var dataProperties = questionForm.data('job-answer-properties');
    if (dataProperties) {
      addCustomProperties(properties, resolveAnswerProperties(dataProperties, questionForm));
    } else {
      addFormInputs(properties, questionForm, button);
    }

    // Add extra values
    var dataPropertiesExtra = questionForm.data('job-answer-properties-extra');
    if (dataPropertiesExtra) {
      addCustomProperties(properties, resolveAnswerProperties(dataPropertiesExtra, questionForm));
    }

    return properties;
  }

  var addCustomProperties = function(properties, extra) {
      $.each(extra, function(key, value) {
        properties['qproperty_' + key] = value;
      });
  };

  var addFormInputs = function(properties, questionForm, button) {
    var entries = questionForm.serializeArray();
    // Add the data from the button that submitted the answer.
    if (button && !button.prop('disabled') && button.attr('name') !== '') {
      entries.push({name: button.attr('name'), value: button.val()});
    }
    entries.each(function(entry) {
      var propertyValue = properties[entry.name];

      if (propertyValue) {
        if (Array.isArray(propertyValue)) {
          propertyValue.push(entry.value);
        } else {
          propertyValue = [propertyValue, entry.value];
        }
      } else {
        propertyValue = entry.value;
      }

      properties[entry.name] = propertyValue;
    });
  };

  var onQuestionAnswer = function(event) {
    // Disable standard form behavior
    event.preventDefault();

    var button = $(this);

    var questionForm = button.parents('.form-question');

    // Disable other buttons
    questionForm.find('btAnswerConfirm').prop('disabled', true);
    questionForm.find('btAnswerCancel').prop('disabled', true);

    if (questionForm.length) {
      var questionUI = questionForm.parents('.ui-question');

      if (questionUI.length) {
        var answerCallback = questionUI.data('answerCallback');

        if (typeof answerCallback === 'function') {
          var createAnswerRequest = questionForm.data('job-answer-createRequest');

          if (createAnswerRequest) {
            answerCallback(createAnswerRequest);
          } else {
            var properties = createAnswerProperties(questionForm, button);

            var answeringNotification = l10n.answering;

            // Set cancel marker if needed
            if (button.hasClass('btAnswerCancel')) {
              properties.cancel = 'true';

              answeringNotification = l10n.canceling;
            }

            var notif = new XWiki.widgets.Notification(
                answeringNotification,
                'inprogress'
              );

            // Send the answer
            answerCallback(properties).then(() => notif.hide());
          }
        }
      }
    }
  };

  var updateStatus = function(job, answerCallback) {
    var jobUI = $(this);
    updateProgress(jobUI, job);
    updateQuestion(jobUI, job, answerCallback);
    updateLog(jobUI, job);
  };

  var notifyJobDone = function(job) {
    var jobUI = $(this);
    jobUI.find('.ui-progress').replaceWith(job.message);
  };

  var notifyConnectionFailure = function() {
  };

  $('.job-status').has('.ui-progress').each(function() {
    var jobStatus = $(this);
    var url = jobStatus.attr('data-url');
    if (url !== '') {
      var jobLog = jobStatus.find('.log');
      var runnerConfig = {};

      runnerConfig.createStatusRequest = function() {
            return {
              url: url,
              data: {
                'logOffset': jobLog.find('.log-item').length
              }
            };
          };

      // Handle various question related actions
      var jobQuestion = jobStatus.find('.ui-question');
      if (jobQuestion.length) {
        jobQuestion.on('click', '.btAnswerConfirm', onQuestionAnswer);
        jobQuestion.on('click', '.btAnswerCancel', onQuestionAnswer);

        runnerConfig.createAnswerRequest = function(jobId, data) {
          if (typeof data === 'function') {
            return data();
          } else {
            var answerURL = XWiki.contextPath + '/job/question/' + jobId.map(encodeURIComponent).join('/');

            return {
              url: answerURL,
              data: data
            };
          }
        };
      }

      new JobRunner(runnerConfig).resume()
        .progress(updateStatus.bind(this))
        .then(notifyJobDone.bind(this), notifyConnectionFailure.bind(this));
    }
  });
});