const sharp = require('sharp');
const path = require('path');

const srcDir = 'C:\\Users\\Lenovo\\.gemini\\antigravity-ide\\brain\\0b09f3da-da11-4f71-83dd-d9d250e7540b';
const destDir = path.join(__dirname, 'public', 'assets', 'images', 'rewards');

const files = [
    { src: 'sobre_comun_1782411909774.png', dest: 'pack_comun.png' },
    { src: 'sobre_raro_1782411918690.png', dest: 'pack_raro.png' },
    { src: 'sobre_epico_1782411928018.png', dest: 'pack_epico.png' },
    { src: 'sobre_legendario_1782411937482.png', dest: 'pack_legendario.png' }
];

async function processImages() {
    for (const file of files) {
        const input = path.join(srcDir, file.src);
        const output = path.join(destDir, file.dest);
        try {
            await sharp(input)
                .trim() // Automatically crops away the solid/transparent background
                .toFile(output);
            console.log(`Trimmed ${file.dest}`);
        } catch (err) {
            console.error(`Error processing ${file.dest}:`, err);
        }
    }
}

processImages();
