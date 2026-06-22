const { Client } = require('pg');
const client = new Client({
  user: 'admin',
  host: 'localhost',
  database: 'pokemon_tcg',
  password: 'adminpassword',
  port: 5432
});

client.connect().then(() => {
  return client.query("SELECT * FROM users;");
}).then(res => {
  console.table(res.rows);
  client.end();
}).catch(err => {
  console.error(err);
});
