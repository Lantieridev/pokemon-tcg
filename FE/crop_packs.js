const sharp = require('sharp');
const path = require('path');

async function cropImages() {
    const dir = path.join(__dirname, 'public', 'assets', 'images', 'rewards');
    
    // The images are likely 1024x1024.
    // The booster pack is in the center. We'll crop out the margins.
    // Let's take x from 256 to 768 (width 512)
    // Let's take y from 100 to 924 (height 824)
    const filesToCrop = ['pack_comun.png', 'pack_raro.png', 'pack_epico.png'];

    for (const file of filesToCrop) {
        try {
            const inputPath = path.join(dir, file);
            const outputPath = path.join(dir, file.replace('.png', '_cropped.png'));
            
            await sharp(inputPath)
                .extract({ left: 280, top: 80, width: 464, height: 864 })
                .toFile(outputPath);
                
            console.log(`Cropped ${file}`);
        } catch (e) {
            console.log(`Error ${file}: ${e.message}`);
        }
    }
}
cropImages();
