// Logs in through the real backend (not the UI) and seeds localStorage the
// same way AuthService does on a normal login, so specs that aren't testing
// the login flow itself don't have to re-drive the login form every time.
Cypress.Commands.add('loginViaApi', (username: string, password: string) => {
  cy.request('POST', 'http://localhost:8081/api/auth/login', { username, password }).then((res) => {
    const { token, username: respUsername, userId } = res.body;
    window.localStorage.setItem('jwt', token);
    window.localStorage.setItem('username', respUsername);
    window.localStorage.setItem('userId', String(userId));
  });
});

declare global {
  namespace Cypress {
    interface Chainable {
      loginViaApi(username: string, password: string): Chainable<void>;
    }
  }
}

export {};
