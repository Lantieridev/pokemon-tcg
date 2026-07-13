describe('Auth', () => {
  it('redirects an unauthenticated visitor to /login', () => {
    cy.visit('/');
    cy.url().should('include', '/login');
    cy.contains('POKÉMON');
  });

  it('registers a new account and lands on the lobby, logged in', () => {
    const username = `e2e_user_${Date.now()}`;

    cy.visit('/login');
    cy.contains('button', 'Registrarse').click();

    cy.get('#login-username').type(username);
    cy.get('#login-email').type(`${username}@example.com`);
    cy.get('#login-password').type('password123');
    cy.get('#login-submit-btn').click();

    cy.url({ timeout: 10000 }).should('include', '/lobby');
    cy.window().its('localStorage.username').should('eq', username);
  });

  it('shows an error and stays on the login screen for wrong credentials', () => {
    cy.visit('/login');
    cy.get('#login-username').type('player-alice');
    cy.get('#login-password').type('definitely-wrong-password');
    cy.get('#login-submit-btn').click();

    cy.contains('Usuario o contraseña incorrectos.', { timeout: 10000 });
    cy.url().should('include', '/login');
  });

  it('logs in an existing seeded user and lands on the lobby', () => {
    cy.visit('/login');
    cy.get('#login-username').type('player-alice');
    cy.get('#login-password').type('dummy');
    cy.get('#login-submit-btn').click();

    cy.url({ timeout: 10000 }).should('include', '/lobby');
  });
});
