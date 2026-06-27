-- Consolidate pack_base and pack_xy_base into pack_comun in the user_packs table

-- 1. For users that have pack_base, add their quantity to pack_comun (if pack_comun exists)
UPDATE user_packs up1
SET quantity = quantity + (SELECT quantity FROM user_packs up2 WHERE up2.user_id = up1.user_id AND up2.pack_type = 'pack_base')
WHERE pack_type = 'pack_comun'
AND EXISTS (SELECT 1 FROM user_packs up2 WHERE up2.user_id = up1.user_id AND up2.pack_type = 'pack_base');

-- 2. For users that have pack_base but NO pack_comun yet, rename pack_base to pack_comun
UPDATE user_packs
SET pack_type = 'pack_comun'
WHERE pack_type = 'pack_base'
AND user_id NOT IN (SELECT user_id FROM user_packs WHERE pack_type = 'pack_comun');

-- 3. Same for pack_xy_base just in case
UPDATE user_packs up1
SET quantity = quantity + (SELECT quantity FROM user_packs up2 WHERE up2.user_id = up1.user_id AND up2.pack_type = 'pack_xy_base')
WHERE pack_type = 'pack_comun'
AND EXISTS (SELECT 1 FROM user_packs up2 WHERE up2.user_id = up1.user_id AND up2.pack_type = 'pack_xy_base');

UPDATE user_packs
SET pack_type = 'pack_comun'
WHERE pack_type = 'pack_xy_base'
AND user_id NOT IN (SELECT user_id FROM user_packs WHERE pack_type = 'pack_comun');

-- 4. Finally, delete the old entries completely
DELETE FROM user_packs WHERE pack_type IN ('pack_base', 'pack_xy_base');
