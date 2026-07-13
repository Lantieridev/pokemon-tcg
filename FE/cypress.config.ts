import { defineConfig } from 'cypress';

export default defineConfig({
  e2e: {
    baseUrl: 'http://localhost:4200',
    supportFile: 'cypress/support/e2e.ts',
    // The card catalog endpoint proxies to an external API with no backend
    // caching (~8-12s on every call, confirmed via direct curl) - the 4s
    // Cypress default is unrealistic against this stack.
    defaultCommandTimeout: 15000,
    requestTimeout: 20000,
    setupNodeEvents(on, config) {
      // implement node event listeners here
    },
  },
});