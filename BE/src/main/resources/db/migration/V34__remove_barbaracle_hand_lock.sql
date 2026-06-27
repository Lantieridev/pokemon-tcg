-- Flyway migration to remove Barbaracle's Hand Lock ability
UPDATE cards 
SET abilities = '[]' 
WHERE id = 'xy2-49';
