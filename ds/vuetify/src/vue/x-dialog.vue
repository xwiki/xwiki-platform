<template>
    <v-dialog vModel="isActive" :width="width">
      <template v-slot:activator="{ props }">
        <span v-bind="props">
          <slot name="activator" />
        </span>
      </template>
      <template v-slot:default="{ isActive }">
        <v-card :title="title">
            <v-card-text>
              <slot name="default" v-bind="isActive" /> 
            </v-card-text>
        </v-card>
      </template>
    </v-dialog>
  </template>
  <script lang="ts">
  export default {
    props: [ "class", "isActive", "title", "width", "logo" ],
    setup(props) {
      console.log("Dialog:", props.isActive);
      return { isActive : true }
    }
  }
  </script>
  