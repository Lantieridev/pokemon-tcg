const sharp = require('sharp');
const path = require('path');

async function cropRaro() {
    const dir = path.join(__dirname, 'public', 'assets', 'images', 'rewards');
    
    try {
        const inputPath = path.join(dir, 'pack_raro.png');
        const outputPath = path.join(dir, 'pack_raro_cropped.png');
        
        // height is 933. Let's extract height 864, top 34
        await sharp(inputPath)
            .extract({ left: 280, top: 34, width: 464, height: 864 })
            .toFile(outputPath);
            
        console.log(`Cropped pack_raro.png`);
    } catch (e) {
        console.log(`Error pack_raro.png: ${e.message}`);
    }
}
cropRaro();
