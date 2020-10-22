<!--
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
 -->

<template>
  <div
    class="modal fade"
    :id="id"
    tabindex="-1"
    role="dialog"
    @click="callCallback()"
  >
    <div
      class="modal-dialog"
      role="document"
    >
      <div class="modal-content">
        <div class="modal-header">
          <button
            type="button"
            class="close"
            data-dismiss="modal"
            aria-label="Close"
            @click="callCallback()"
          >
            <span aria-hidden="true">&times;</span>
          </button>
          <h4 class="modal-title">{{ title }}</h4>
        </div>
        <div class="modal-body">
          <p>{{ text }}</p>
        </div>
        <div class="modal-footer">
          <button
            v-for="button in buttons"
            :key="button.id"
            type="button"
            :class="['btn', 'btn-' + (button.variant || 'default')]"
            aria-label="Close"
            data-dismiss="modal"
            @click="callCallback(button.id)"
          >{{ button.text }}</button>
        </div>
      </div>
    </div>
  </div>
</template>


<script>
import Vue from "vue";
import $ from "jquery";

const XWikiDialog = {

  name: "XWikiDialog",

  props: {
    // custom id of the modal (optionnal)
    id: String,
    // title of the modal
    title: String,
    // text displayed as modal content, if no slot is given
    text: String,
    /*
      The buttons of the modal are object like: {
        id: id of the button
        (you can let the id undefined for 'cancel' button so that
        it act the same as closing the modal)
        text: text displayed in the button
        variant: bootstrap variant keyword (default: "default")
        callback: function to be called after click on the button
      }
    */
    buttons: Array,
    // Global callback that happens on button click or modal leave
    // The passed argument is the id of the click button, or undefined
    callback: Function,
  },

  methods: {
    callCallback (buttonId) {
      // call global callback
      if (typeof this.callback === "function") {
        this.callback(buttonId);
      }
      // call specific button callback
      const button = this.buttons?.find(button => button.id === buttonId);
      if (typeof button?.callback === "function") {
        button.callback();
      }
    },
  },

};
export default XWikiDialog;


/**
 *
 */
export const dialogFactory = function (dialogComponent) {
  return function (paramObject) {
    return new Promise (resolve => {
      // callback parameter
      paramObject.callback = function (buttonId) {
        resolve(buttonId);
      };
      // Programatically create the dialog vue component
      const dialogClass = Vue.extend(dialogComponent);
      const dialogInstance = new dialogClass({
          propsData: paramObject,
      });
      dialogInstance.$mount();
      // Insert component in body
      document.body.appendChild(dialogInstance.$el);
      $(dialogInstance.$el).modal("show");
      // Remove component when modal is hidden
      $(dialogInstance.$el).on("hidden.bs.modal", () => {
        dialogInstance.$el.remove();
        dialogInstance.$destroy();
      });
    });

  };
};

/**
 * Display a dialog modal using the XWikiDialog component
 * It is asyncronous and return a promise
 * @param {paramObject} paramObject An object containing the modal configuration
 * (that is the props to be passed to the created XWikiDialog component)
 * @returns {Promise}
 */
export const dialog = dialogFactory(XWikiDialog);

</script>


<style>

</style>
