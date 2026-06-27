const sharp = require('sharp');
const path = require('path');

const srcDir = 'C:\\Users\\Lenovo\\.gemini\\antigravity-ide\\brain\\0b09f3da-da11-4f71-83dd-d9d250e7540b';
const destDir = path.join(__dirname, 'public', 'assets', 'images', 'rewards');

const files = [
    { src: 'sobre_comun_1782411909774.png', dest: 'pack_comun.png' },
    { src: 'sobre_raro_1782411918690.png', dest: 'pack_raro.png' },
    { src: 'sobre_epico_1782411928018.png', dest: 'pack_epico.png' }
];

function isBackground(r, g, b, a) {
    if (a < 50) return true; // transparent
    // Treat light colors (white, light gray) as background since they were drawn as studio background/checkerboard
    if (r > 200 && g > 200 && b > 200) return true;
    if (Math.abs(r - g) < 15 && Math.abs(g - b) < 15 && r > 170) return true;
    return false;
}

async function autoCrop() {
    for (const f of files) {
        const input = path.join(srcDir, f.src);
        const output = path.join(destDir, f.dest);
        
        try {
            const { data, info } = await sharp(input)
                .ensureAlpha()
                .raw()
                .toBuffer({ resolveWithObject: true });
                
            let minX = info.width, maxX = 0, minY = info.height, maxY = 0;
            
            for (let y = 0; y < info.height; y++) {
                for (let x = 0; x < info.width; x++) {
                    const idx = (y * info.width + x) * 4;
                    const r = data[idx];
                    const g = data[idx+1];
                    const b = data[idx+2];
                    const a = data[idx+3];
                    
                    if (!isBackground(r, g, b, a)) {
                        if (x < minX) minX = x;
                        if (x > maxX) maxX = x;
                        if (y < minY) minY = y;
                        if (y > maxY) maxY = y;
                    }
                }
            }
            
            // Filter out stray pixels by doing a second pass or just use the bounds if they make sense
            // Shadows might stretch maxY. We'll let it be for now.
            let width = maxX - minX + 1;
            let height = maxY - minY + 1;
            
            // Check if bounds are somewhat reasonable for a pack
            // If they are too wide, it means there's noise. Let's just constrain the width to max 500
            if (width > 550) {
                 // assume center is 512
                 minX = 512 - 240;
                 width = 480;
            }
            // constrain height
            if (height > 900) {
                 // assume center is 512
                 minY = 512 - 400;
                 height = 800;
            }
            
            // To ensure we definitely cut off the white bottom, let's trim an extra 10 pixels from bottom
            height -= 10;
            // Trim 5 from top
            minY += 5;
            height -= 5;
            
            // Trim 5 from sides
            minX += 5;
            width -= 10;
            
            console.log(`${f.dest} bounds: left=${minX}, top=${minY}, width=${width}, height=${height}`);
            
            await sharp(input)
                .extract({ left: minX, top: minY, width: width, height: height })
                .toFile(output);
            console.log(`Auto-cropped ${f.dest}`);
            
        } catch (e) {
            console.log(`Error ${f.dest}:`, e);
        }
    }
}
autoCrop();
