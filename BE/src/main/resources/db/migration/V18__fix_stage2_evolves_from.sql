-- Fix incorrect evolves_from values for Stage 2 pokemon
UPDATE cards SET evolves_from = 'Quilladin' WHERE id = 'xy1-14';
UPDATE cards SET evolves_from = 'Braixen' WHERE id = 'xy1-26';
