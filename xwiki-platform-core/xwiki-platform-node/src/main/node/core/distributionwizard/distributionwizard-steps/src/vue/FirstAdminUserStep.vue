<!--
  See the NOTICE file distributed with this work for additional
  information regarding copyright ownership.

  This is free software; you can redistribute it and/or modify it
  under the terms of the GNU Lesser General Public License as
  published by the Free Software Foundation; either version 2.1 of
  the License, or (at your option) any later version.

  This software is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General Public
  License along with this software; if not, write to the Free
  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
  02110-1301 USA, or see the FSF site: http://www.fsf.org.
-->
<script setup lang="ts">
import { ref, watch } from "vue";
const emit = defineEmits(["validateStep", "invalidateStep"]);
const firstname = ref();
const lastname = ref();
const username = ref();
const password = ref();
const password2 = ref();
const email = ref();
const error = ref();

watch(
  [username, password, password2],
  (
    [newUsername, newPassword, newPassword2],
    [previousUsername, previousPassword, previousPassword2],
  ) => {
    if (newUsername === "") {
      error.value = "Username is required";
    } else if (newPassword !== newPassword2) {
      error.value = "Passwords are not matching";
    } else {
      error.value = "";
    }
    if (error.value === "") {
      emit("validateStep");
    } else {
      emit("invalidateStep");
    }
  },
);
const restURL = `${XWiki.contextPath}/rest/distributionWizard/${encodeURIComponent(XWiki.currentWiki)}/step/FirstAdminUserStep`;
async function stepAnswerCallback() {
  console.log("FirstAdminUserStep callback!");
  const response = await fetch(restURL, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      username: username.value,
      password: password.value,
      first_name: firstname.value,
      last_name: lastname.value,
      email: email.value,
    }),
  });
  return response.status >= 200 && response.status < 300;
}
defineExpose({
  stepAnswerCallback,
});
</script>

<template>
  <div class="errors">{{ error }}</div>
  <form id="first-user" method="post" class="xform">
    <dl>
      <dt><label for="register_first_name">First Name</label></dt>
      <dd>
        <input
          type="text"
          id="register_first_name"
          name="first_name"
          v-bind="firstname"
        />
      </dd>
    </dl>
    <dl>
      <dt><label for="register_last_name">Last Name</label></dt>
      <dd>
        <input
          type="text"
          id="register_last_name"
          name="last_name"
          v-bind="lastname"
        />
      </dd>
    </dl>
    <dl>
      <dt><label for="register_username">Username</label></dt>
      <dd>
        <input
          type="text"
          id="register_username"
          name="username"
          class="required"
          v-model="username"
        />
      </dd>
    </dl>
    <dl>
      <dt><label for="register_email">Email</label></dt>
      <dd>
        <input type="text" id="register_email" name="email" v-model="email" />
      </dd>
    </dl>
    <dl>
      <dt><label for="register_password">Password</label></dt>
      <dd>
        <input
          type="password"
          id="register_password"
          name="password"
          class="required"
          v-model="password"
        />
      </dd>
    </dl>
    <dl>
      <dt><label for="register_password2">Password confirmation</label></dt>
      <dd>
        <input
          type="password"
          id="register_password2"
          name="password2"
          v-model="password2"
        />
      </dd>
    </dl>
  </form>
</template>

<style scoped></style>
