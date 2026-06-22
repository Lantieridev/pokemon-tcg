
const getRewardImage = (type, value) => {
    if (!type) return '';
    if (value) {
      let prefix = '';
      if (type.toUpperCase() === 'AVATAR') prefix = 'avatar_';
      if (type.toUpperCase() === 'PACK') prefix = 'pack_';
      if (type.toUpperCase() === 'TITLE') prefix = 'titulo_';
      
      const normalizedValue = value.toLowerCase()
        .normalize('NFD').replace(/[\u0300-\u036f]/g, '')
        .replace(/\s+/g, '_')
        .replace(/[^a-z0-9_]/g, '');
        
      return prefix + normalizedValue + '.png';
    }
}
console.log(getRewardImage('AVATAR', 'Zoroark Ilusión'));
console.log(getRewardImage('TITLE', 'Campeón de Liga'));
console.log(getRewardImage('PACK', 'Unova Dragones'));
console.log(getRewardImage('AVATAR', 'Snivy Planta'));
console.log(getRewardImage('AVATAR', 'Volcarona Solar'));
