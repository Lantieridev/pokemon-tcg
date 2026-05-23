ALTER TABLE cards ADD COLUMN evolves_from VARCHAR(100);

UPDATE cards SET evolves_from = 'Venusaur-EX' WHERE id = 'xy1-2';
UPDATE cards SET evolves_from = 'Weedle' WHERE id = 'xy1-4';
UPDATE cards SET evolves_from = 'Kakuna' WHERE id = 'xy1-5';
UPDATE cards SET evolves_from = 'Ledian' WHERE id = 'xy1-7';
UPDATE cards SET evolves_from = 'Simisage' WHERE id = 'xy1-11';
UPDATE cards SET evolves_from = 'Quilladin' WHERE id = 'xy1-13';
UPDATE cards SET evolves_from = 'Chesnaught' WHERE id = 'xy1-14';
UPDATE cards SET evolves_from = 'Scatterbug' WHERE id = 'xy1-16';
UPDATE cards SET evolves_from = 'Spewpa' WHERE id = 'xy1-17';
UPDATE cards SET evolves_from = 'Gogoat' WHERE id = 'xy1-19';
UPDATE cards SET evolves_from = 'Magcargo' WHERE id = 'xy1-21';
UPDATE cards SET evolves_from = 'Simisear' WHERE id = 'xy1-23';
UPDATE cards SET evolves_from = 'Braixen' WHERE id = 'xy1-25';
UPDATE cards SET evolves_from = 'Delphox' WHERE id = 'xy1-26';
UPDATE cards SET evolves_from = 'Fletchinder' WHERE id = 'xy1-27';
