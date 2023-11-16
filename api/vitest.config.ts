import { defineConfig } from "vite";

export default defineConfig({
  test: {
    reporters: ["junit"],
    outputFile: "unit-tests.xml",
  },
});
