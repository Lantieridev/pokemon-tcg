describe('Bot battle', () => {
  beforeEach(() => {
    // player-alice is a seeded user (see BE DatabaseSeeder/ExampleDeckSeeder)
    // that already owns valid 60-card decks, so this spec can jump straight
    // to starting a match instead of re-building a deck from scratch.
    cy.loginViaApi('player-alice', 'dummy').then(() => {
      // Not testing the onboarding tutorial here - mark it seen so it
      // doesn't cover the lobby UI.
      window.localStorage.setItem('tutorial_visto_player-alice_lobby', 'true');
    });
  });

  it('starts a bot match from the lobby and reaches the battle board', () => {
    cy.visit('/lobby');

    cy.contains('button', 'Jugar vs Bot').click();
    cy.get('#deck-selector', { timeout: 10000 }).should('be.visible');

    cy.contains('button', 'Iniciar Partida').click();

    cy.url({ timeout: 15000 }).should('include', '/battle/');
    cy.contains('TURNO', { timeout: 15000 });
    cy.contains('Abandonar');
  });
});
