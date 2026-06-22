
const fs = require('fs');
const dir = 'C:\\\\Users\\\\Lenovo\\\\OneDrive\\\\Desktop\\\\tpi-pokemon-2w1-15\\\\FE\\\\public\\\\assets\\\\achievements\\\\avatars';
const files = fs.readdirSync(dir);
const testNames = [
  'avatar_winner_badge', 'avatar_rules_student', 'avatar_resilience_mid', 
  'avatar_neutral_balance', 'avatar_belt_white', 'avatar_water_kanto',
  'Entrenador Novato', 'Pikachu Alegre', 'Gengar Sombra', 'Charmander Fuego', 'Squirtle Burbujas',
  'Bulbasaur Básico', 'Gran Mentor', 'Líder de Élite', 'Maestro de Kanto',
  'Ganador Implacable', 'Inmortal del Tablero', 'Fuerza de Voluntad', 'Leyenda de Batallas',
  'Espíritu Inquebrantable', 'Curador del Museo', 'Líder de Gimnasio', 'Estratega Versátil',
  'Maestro Adaptable', 'Multifacético', 'Celebridad de Kanto', 'Fuerza de la Naturaleza',
  'Ejecutor Implacable', 'Estudioso de Reglas', 'Llama de Kanto', 'Tsunami Viviente',
  'Espíritu del Bosque', 'Tormenta Perpetua', 'Poder Cósmico', 'Cinturón Blanco',
  'Fuerza Sísmica', 'Equilibrio', 'Armonía Pura', 'Amigo del Ratón', 'Aliento Ígneo',
  'Presión de Agua', 'Floración Rápida', 'Mirada Mental'
];
testNames.forEach(name => {
  const normalizedValue = name.toLowerCase()
      .normalize('NFD').replace(/[\u0300-\u036f]/g, '')
      .replace(/\s+/g, '_')
      .replace(/[^a-z0-9_]/g, '');
  const prefix = normalizedValue.startsWith('avatar_') ? '' : 'avatar_';
  const filename = prefix + normalizedValue + '.png';
  if (!files.includes(filename)) {
    console.log('BROKEN:', name, '->', filename);
  }
});
