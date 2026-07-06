-- Stardust was granted by the battle pass but had no spend path anywhere in the
-- app (no store item, no shop tab), and after V25 it wasn't even granted anymore
-- either. Removing the dead currency instead of leaving a column nothing reads.
ALTER TABLE users DROP COLUMN IF EXISTS stardust;
