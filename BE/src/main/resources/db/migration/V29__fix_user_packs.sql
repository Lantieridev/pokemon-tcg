UPDATE user_packs SET pack_type = 'pack_comun' WHERE pack_type IN ('pack_base', 'pack_xy_base') AND NOT EXISTS (SELECT 1 FROM user_packs up2 WHERE up2.user_id = user_packs.user_id AND up2.pack_type = 'pack_comun');
DELETE FROM user_packs WHERE pack_type IN ('pack_base', 'pack_xy_base');
