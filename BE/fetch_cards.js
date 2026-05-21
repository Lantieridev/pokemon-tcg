const fs = require('fs');
const https = require('https');
const path = require('path');

const url = 'https://api.pokemontcg.io/v2/cards?q=set.id:xy1';

https.get(url, (res) => {
  let body = '';
  res.on('data', chunk => body += chunk);
  res.on('end', () => {
    const data = JSON.parse(body).data;
    
    const dir = path.join(__dirname, 'src', 'main', 'resources', 'db', 'migration');
    fs.mkdirSync(dir, { recursive: true });
    
    const filePath = path.join(dir, 'V2__seed_xy1_cards.sql');
    let sql = "-- Seed data for Pokemon TCG XY1 Set\n\n";
    sql += "INSERT INTO cards (id, name, supertype, subtype, hp, rules, attacks, weaknesses, resistances, retreat_cost, set_id) VALUES\n";
    
    const values = data.map(card => {
      const c_id = card.id || '';
      const c_name = (card.name || '').replace(/'/g, "''");
      const c_supertype = card.supertype || '';
      const c_subtype = (card.subtypes || []).join(', ');
      
      const hp_str = card.hp || '0';
      const c_hp = parseInt(hp_str, 10) || 0;
      
      const c_rules = card.rules ? `$$${JSON.stringify(card.rules)}$$` : "'[]'";
      const c_attacks = card.attacks ? `$$${JSON.stringify(card.attacks)}$$` : "'[]'";
      const c_weaknesses = card.weaknesses ? `$$${JSON.stringify(card.weaknesses)}$$` : "'[]'";
      const c_resistances = card.resistances ? `$$${JSON.stringify(card.resistances)}$$` : "'[]'";
      const c_retreat_cost = card.retreatCost ? `$$${JSON.stringify(card.retreatCost)}$$` : "'[]'";
      const c_set_id = 'xy1';
      
      return `('${c_id}', '${c_name}', '${c_supertype}', '${c_subtype}', ${c_hp}, ${c_rules}, ${c_attacks}, ${c_weaknesses}, ${c_resistances}, ${c_retreat_cost}, '${c_set_id}')`;
    });
    
    sql += values.join(',\n') + ';\n';
    
    fs.writeFileSync(filePath, sql, 'utf-8');
    console.log(`V2__seed_xy1_cards.sql generated successfully with ${data.length} cards.`);
  });
}).on('error', (e) => {
  console.error(e);
});
