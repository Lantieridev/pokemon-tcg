-- Reduce the prices of store packs by half to make them more accessible
UPDATE store_items SET price = 500 WHERE image_url = 'pack_comun';
UPDATE store_items SET price = 1250 WHERE image_url = 'pack_raro';
UPDATE store_items SET price = 2500 WHERE image_url = 'pack_epico';
UPDATE store_items SET price = 5000 WHERE image_url = 'pack_legendario';
