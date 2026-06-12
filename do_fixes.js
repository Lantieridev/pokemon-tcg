const fs = require('fs');

let battle = fs.readFileSync('FE/src/app/features/battle/battle.component.ts', 'utf8');
battle = battle.split(supertype === 'Pok\u01F8mon').join(supertype === 'Pokémon');
fs.writeFileSync('FE/src/app/features/battle/battle.component.ts', battle);

let models = fs.readFileSync('FE/src/app/core/models/game-state.models.ts', 'utf8');
models = models.split(supertype: 'Pok\u01F8mon' | 'Trainer' | 'Energy';).join(supertype: 'Pokémon' | 'Trainer' | 'Energy';);
fs.writeFileSync('FE/src/app/core/models/game-state.models.ts', models);

