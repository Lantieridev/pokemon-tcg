-- V17: Add abilities to XY1 set Pokémon

-- Trevenant (xy1-55)
UPDATE cards SET abilities = '[{"name": "Forest''s Curse", "text": "As long as this Pokémon is your Active Pokémon, your opponent can''t play any Item cards from their hand.", "type": "Ability"}]' WHERE id = 'xy1-55';

-- Furfrou (xy1-114)
UPDATE cards SET abilities = '[{"name": "Fur Coat", "text": "Any damage done to this Pokémon by attacks is halved (after applying Weakness and Resistance) (rounded up to the nearest 10).", "type": "Ability"}]' WHERE id = 'xy1-114';

-- Swellow (xy1-103)
UPDATE cards SET abilities = '[{"name": "Drive Off", "text": "Once during your turn (before your attack), if this Pokémon is your Active Pokémon, you may have your opponent switch his or her Active Pokémon with 1 of his or her Benched Pokémon.", "type": "Ability"}]' WHERE id = 'xy1-103';

-- Chesnaught (xy1-14)
UPDATE cards SET abilities = '[{"name": "Spiky Shield", "text": "If this Pokémon is your Active Pokémon and is damaged by an opponent''s attack (even if this Pokémon is Knocked Out), put 3 damage counters on the Attacking Pokémon.", "type": "Ability"}]' WHERE id = 'xy1-14';

-- Voltorb (xy1-44)
UPDATE cards SET abilities = '[{"name": "Destiny Burst", "text": "If this Pokémon is your Active Pokémon and is Knocked Out by damage from an opponent''s attack, flip a coin. If heads, put 5 damage counters on the Attacking Pokémon.", "type": "Ability"}]' WHERE id = 'xy1-44';

-- Greninja (xy1-41)
UPDATE cards SET abilities = '[{"name": "Water Shuriken", "text": "Once during your turn (before your attack), you may discard a Water Energy card from your hand. If you do, put 3 damage counters on 1 of your opponent''s Pokémon.", "type": "Ability"}]' WHERE id = 'xy1-41';

-- Inkay (xy1-74)
UPDATE cards SET abilities = '[{"name": "Upside-Down Evolution", "text": "If this Pokémon is Confused, you may search your deck for a card that evolves from this Pokémon and put it onto this Pokémon to evolve it. Shuffle your deck afterward.", "type": "Ability"}]' WHERE id = 'xy1-74';

-- Aegislash (xy1-85)
UPDATE cards SET abilities = '[{"name": "Stance Change", "text": "Once during your turn (before your attack), you may switch this Pokémon with an Aegislash in your hand. (Any cards attached to this Pokémon, damage counters, Special Conditions, and turns in play remain on the new Pokémon.)", "type": "Ability"}]' WHERE id = 'xy1-85';

-- Aegislash (xy1-86)
UPDATE cards SET abilities = '[{"name": "Stance Change", "text": "Once during your turn (before your attack), you may switch this Pokémon with an Aegislash in your hand. (Any cards attached to this Pokémon, damage counters, Special Conditions, and turns in play remain on the new Pokémon.)", "type": "Ability"}]' WHERE id = 'xy1-86';
