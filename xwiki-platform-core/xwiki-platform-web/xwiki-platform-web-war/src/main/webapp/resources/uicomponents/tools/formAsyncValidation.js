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
define('xwiki-form-validation-async', ['jquery'], function($) {
  // Enable or disable the first submit button from the specifid HTML form.
  const enableSubmit = (form, enabled) => {
    form.find('input[type="submit"], button[type="submit"]').first().prop('disabled', !enabled);
  };

  // Enable or disable an HTML form using its child fieldset element.
  const enableForm = (form, enabled) => {
    const fieldSet = form.children('fieldset');
    const previousState = !fieldSet.prop('disabled');
    fieldSet.prop('disabled', !enabled);
    return previousState;
  };

  const hasValidationsWithState = function(validations, states) {
    return validations.some(validation => {
      return states.indexOf(validation.__state) >= 0;
    });
  };

  // Postpone the form submit while there are pending or rejected validations. We use event capturing because we want to
  // execute our code before other form submit listeners (that are usually registered on the bubbling phase). We use
  // plain JavaScript to register our listener because jQuery doesn't support the capturing phase.
  document.addEventListener('submit', event => {
    // Check if there are pending or rejected validations for the target form.
    const form = $(event.target);
    const submitter = event.submitter;
    const asyncValidations = Object.values(form.data('asyncValidations'));
    if (hasValidationsWithState(Object.values(asyncValidations), ['pending', 'rejected'])) {
      // Postpone the submit until the pending validations are resolved.
      event.preventDefault();
      // Stop the event propagation (we're in the capturing phase) because the form is not ready yet to be submitted.
      event.stopPropagation();
      // Disable the form in order to prevent the user from re-submitting it while waiting for pending validations to be
      // resolved.
      enableForm(form, false);
      Promise.all(asyncValidations).finally(() => {
        // Re-enable the form so that:
        // * the user can fix the validation errors, if the validation failed
        // * the form data can be submitted, if there are no validation errors
        enableForm(form, true);
      }).then(() => {
        // All asynchronous validations have been resolved. Resume the submit.
        if (submitter) {
          // Use the button that initially triggered the submit in order to include its value in the sent data.
          $(submitter).click();
        } else {
          // Fallback on submitting the form without a particular button.
          form.submit();
        }
      }).catch(() => {
        // Form validation failed. Abort the submit.
      });
    }
  }, true);

  // Schedule a validation with the specified delay.
  const delayedValidation = function(nextValidation, delay, form) {
    let abort, timeout;
    const validation = new Promise((resolve, reject) => {
      abort = () => {
        clearTimeout(timeout);
        reject();
      };
      timeout = setTimeout(() => {
        // Make sure the form is enabled when the validation starts in case the validation needs to serialize the form
        // or some of its form fields. We restore the state afterwards.
        const enabled = enableForm(form, true);
        Promise.resolve(nextValidation()).then(resolve, reject);
        // Restore the form state because the form may have been disabled by the submit handler.
        enableForm(form, enabled);
      }, delay);
    });

    // We need a way to abort the previous validations and the Promise API doesn't help us.
    validation.__abort = abort;

    // There's no public API to check the state of a promise so we have to hack it.
    validation.__state = 'pending';
    validation.then(() => {
      validation.__state = 'fulfilled';
    }).catch(() => {
      validation.__state = 'rejected';
    });

    return validation;
  };

  const wrapValidation = function(nextValidation, delay, form) {
    if (typeof nextValidation === 'function') {
      if (!(delay >= 0)) {
        delay = 0;
      }
      // The validation is specified as a function that returns a promise, and will be executed with the specified
      // delay (unless another validation is scheduled in the mean time).
      return delayedValidation(nextValidation, delay, form);
    } else {
      // The validation is specified as a promise, so it already started but might not be finished yet.
      return delayedValidation(() => nextValidation, 0, form);
    }
  };

  const validateAsync = ({form, validation, validationKey, delay}) => {
    const asyncValidations = form.data('asyncValidations') || {};
    form.data('asyncValidations', asyncValidations);

    // Replace and then abort the previous validation associated with the given key.
    const previousValidation = asyncValidations[validationKey];
    const nextValidation = wrapValidation(validation, delay, form);
    asyncValidations[validationKey] = nextValidation;
    previousValidation?.__abort();

    // Construct a promise that is settled when the next validation is settled, if not outdated.
    const afterValidation = new Promise((resolve, reject) => {
      nextValidation.then((value) => {
        if (asyncValidations[validationKey] === nextValidation) {
          resolve(value);
        }
      }).catch((error) => {
        if (asyncValidations[validationKey] === nextValidation) {
          reject(error);
        }
      });
    });

    // Disable the form submit when the validation fails, if the validation is not outdated.
    afterValidation.catch(() => {
      enableSubmit(form, false);
    });

    // Re-enable the submit button while the next validation is in progress, if there are no other failed validations.
    enableSubmit(form, !hasValidationsWithState(Object.values(asyncValidations), ['rejected']));

    return afterValidation;
  };

  /**
   * Schedules an asynchronous validation for a form field or marks the fact that a form field is currently being
   * validated asynchronously. Can be used like this:
   *
   * <ul>
   *   <li>$('#myFormField').validateAsync(() => Promise.resolve(), 500, 'myApp')</li>
   *   <li>$('#myFormField').validateAsync(() => Promise.resolve(), 500)</li>
   *   <li>$('#myFormField').validateAsync(Promise.resolve(), 'myApp')</li>
   *   <li>$('#myFormField').validateAsync(Promise.resolve())</li>
   * </ul>
   *
   * @param validation either a promise that is fulfilled when the validation is successful and rejected when the
   *   validation fails, or a function that returns such a promise (in case the validation needs to be delayed)
   * @param delay the amount of milliseconds to delay the validation, used only when the validation is specified as a
   *   function that returns a promise
   * @param namespace for each validation a key is computed based on the form field id and this namespace, in order to
   *   be able to abort and overwrite a previous validation for the same form field
   * @return the validation promise
   **/
  $.fn.validateAsync = function(validation, delay, namespace) {
    let validationKey = this.attr('id') || this.attr('name');
    if (!validationKey) {
      throw 'The id and name attributes are both missing from the target form field.';
    }
    if (arguments.length === 2 && typeof delay === 'string') {
      namespace = delay;
      delay = undefined;
    }
    if (namespace) {
      validationKey = `${namespace}.${validationKey}`;
    }
    return validateAsync({form: this.closest('form'), validation, validationKey, delay});
  };
});
