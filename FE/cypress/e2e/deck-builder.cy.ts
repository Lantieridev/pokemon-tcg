describe('Deck Builder', () => {
  beforeEach(() => {
    const username = `e2e_deck_${Date.now()}`;
    cy.request('POST', 'http://localhost:8081/api/auth/register', {
      username,
      email: `${username}@example.com`,
      password: 'password123',
    });
    cy.loginViaApi(username, 'password123').then(() => {
      // Not testing the onboarding tutorial here - mark it seen so it doesn't
      // cover the deck-builder UI for this brand-new account.
      window.localStorage.setItem(`tutorial_visto_${username}_deck`, 'true');
    });
  });

  it('builds a 60-card deck via autocomplete and saves it as valid', () => {
    cy.intercept('GET', '**/api/cards/catalog*').as('catalog');
    cy.visit('/deck');
    // No backend caching on this endpoint - it proxies to an external API
    // and reliably takes several seconds.
    cy.wait('@catalog', { timeout: 60000 });

    cy.contains('.create-card', 'Crear Mazo').click();
    cy.contains('.mode-card', 'Personalizado').click();

    cy.get('input[placeholder="Nombre del mazo"]')
      .clear()
      .type('Mazo E2E');

    // The button is disabled until the card catalog finishes loading
    // (fixed race condition: autoComplete() used to silently no-op if
    // clicked before tcgService.cards() was populated).
    cy.get('#boton-autocompletar').should('not.be.disabled').click();
    cy.contains('60/60', { timeout: 15000 });

    cy.contains('button', 'Guardar').should('not.be.disabled').click();
    cy.contains('¡Guardado exitoso!', { timeout: 10000 });
  });

  it('cannot save as a valid deck while under 60 cards', () => {
    cy.visit('/deck');

    cy.contains('.create-card', 'Crear Mazo').click();
    cy.contains('.mode-card', 'Personalizado').click();

    cy.contains('button', 'Guardar').should('be.disabled');
    cy.contains('Te faltan 60 cartas para llegar a 60.', { timeout: 10000 });
  });
});
