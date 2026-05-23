ALTER TABLE cards ADD COLUMN abilities JSONB;

UPDATE cards SET abilities = '[{"name": "Mystical Fire", "text": "Once during your turn (before your attack), you may draw cards until you have 6 cards in your hand.", "type": "Ability"}]' WHERE id = 'xy1-26';

UPDATE cards SET abilities = '[{"name": "Magnetic Draw", "text": "Once during your turn (before your attack), you may draw cards until you have 4 cards in your hand.", "type": "Ability"}]' WHERE id = 'xy1-40';

UPDATE cards SET abilities = '[{"name": "Fairy Transfer", "text": "As often as you like during your turn (before your attack), you may move a Fairy Energy attached to 1 of your Pokémon to another of your Pokémon.", "type": "Ability"}]' WHERE id = 'xy1-93';

UPDATE cards SET abilities = '[{"name": "Sweet Veil", "text": "Each of your Pokémon that has any Fairy Energy attached to it can''t be affected by any Special Conditions.", "type": "Ability"}]' WHERE id = 'xy1-95';
