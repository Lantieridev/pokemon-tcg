const fs = require('fs');

const premiumTypes = ['COINS', 'PACK', 'TITLE', 'AVATAR'];
const freeTypes = ['COINS', 'PACK'];

let sql = `CREATE TABLE IF NOT EXISTS battle_pass_levels (
    level INT PRIMARY KEY,
    required_xp INT NOT NULL,
    free_reward_type VARCHAR(50),
    free_reward_amount INT DEFAULT 0,
    free_reward_value VARCHAR(100),
    premium_reward_type VARCHAR(50),
    premium_reward_amount INT DEFAULT 0,
    premium_reward_value VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS user_battle_pass (
    user_id BIGINT PRIMARY KEY,
    is_premium BOOLEAN DEFAULT FALSE,
    claimed_free_level INT DEFAULT 0,
    claimed_premium_level INT DEFAULT 0,
    CONSTRAINT fk_user_battle_pass_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Seeding some levels
INSERT INTO battle_pass_levels (level, required_xp, free_reward_type, free_reward_amount, free_reward_value, premium_reward_type, premium_reward_amount, premium_reward_value) VALUES
`;

const values = [];

for (let i = 1; i <= 50; i++) {
    const requiredXp = Math.floor(100 + Math.pow(i, 1.8) * 45); // Progressive curve
    
    // Free Track (Every 4 levels, plus level 1)
    let fType = 'null';
    let fAmt = 'null';
    let fVal = 'null';
    if (i === 1 || i % 4 === 0 || i === 50) {
        if (i % 8 === 0) {
            fType = "'PACK'";
            fAmt = '1';
        } else if (i === 50) {
            fType = "'AVATAR'";
            fAmt = '1';
            fVal = "'ash'";
        } else if (i % 12 === 0) {
            fType = "'STARDUST'";
            fAmt = '200';
        } else {
            fType = "'COINS'";
            fAmt = '50';
        }
    }

    // Premium Track (Every level)
    let pType = "'COINS'";
    let pAmt = '100';
    let pVal = 'null';
    
    if (i % 10 === 0) {
        pType = "'AVATAR'";
        pAmt = '1';
        pVal = `'avatar_premium_${i}'`;
    } else if (i % 5 === 0) {
        pType = "'TITLE'";
        pAmt = '1';
        pVal = `'Maestro Lvl ${i}'`;
    } else if (i % 3 === 0) {
        pType = "'STARDUST'";
        pAmt = (i * 20).toString();
    } else if (i % 2 === 0) {
        pType = "'PACK'";
        pAmt = (i < 20) ? '1' : '2';
    } else {
        pType = "'COINS'";
        pAmt = (i * 10).toString();
    }

    values.push(`(${i}, ${requiredXp}, ${fType}, ${fAmt}, ${fVal}, ${pType}, ${pAmt}, ${pVal})`);
}

sql += values.join(',\n') + ';';

fs.writeFileSync('BE/src/main/resources/db/migration/V23__add_battle_pass.sql', sql);
console.log('V23 rewritten');
