-- Flyway migration to fix Barbaracle's ability (Hand Lock)
UPDATE cards 
SET abilities = '[{"name": "Hand Lock", "text": "As long as this Pokémon is in play, your opponent can''t attach Special Energy cards from his or her hand to his or her Pokémon.", "type": "Ability"}]' 
WHERE id = 'xy2-49';
