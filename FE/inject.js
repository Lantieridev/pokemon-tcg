const { Client } = require('pg');
const client = new Client({
  user: 'admin',
  host: 'localhost',
  database: 'pokemon_tcg',
  password: 'adminpassword',
  port: 5432
});

client.connect().then(() => {
  return client.query("UPDATE users SET xp = 0, level = 1, pokecoins = 0;");
}).then(res => {
  console.log('Reset rows:', res.rowCount);
  client.end();
}).catch(err => {
  console.error(err);
});
