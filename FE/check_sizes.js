const sharp = require('sharp');
const dir = 'C:/Users/Lenovo/OneDrive/Desktop/tpi-pokemon-2w1-15/FE/public/assets/images/rewards/';
['pack_sinnoh_mistico.png', 'pack_johto_retro.png', 'pack_hoenn_avanzado.png'].forEach(f => {
  sharp(dir + f).metadata().then(md => console.log(f + ': ' + md.width + 'x' + md.height)).catch(e => console.log(e));
});
