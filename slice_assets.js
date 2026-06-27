"const fs = require('fs');
const path = require('path');
const Jimp = require('jimp');

// Output paths
const MEDALS_DIR = "c:\\Users\\lucas\\.gemini\\antigravity\\scratch\\tpi-pokemon-2w1-15\\FE\\public\\assets\\achievements\\medals";
const AVATARS_DIR = "c:\\Users\\lucas\\.gemini\\antigravity\\scratch\\tpi-pokemon-2w1-15\\FE\\public\\assets\\achievements\\avatars";

if (!fs.existsSync(MEDALS_DIR)) {
    fs.mkdirSync(MEDALS_DIR, { recursive: true });
}
if (!fs.existsSync(AVATARS_DIR)) {
    fs.mkdirSync(AVATARS_DIR, { recursive: true });
}

// Grid definitions
const grids = [
    // 1. Flat Medals (4x4)
    {
        imagePath: "C:\\Users\\lucas\\.gemini\\antigravity\\brain\\fa8f899c-4945-49f1-8fc7-a96784c2067f\\medals_flat_grid_1780688383528.png",
        cols: 4,
        rows: 4,
        names: [
            "medal_veteran", "medal_collector_elite", "medal_coins_1k", "medal_magnate_gold",
            "medal_power_1k", "medal_kos_10", "medal_fire_50", "medal_fire_200",
            "medal_water_50", "medal_water_200", "medal_grass_50", "medal_grass_200",
            "medal_lightning_50", "medal_lightning_200", "medal_psychic_50", "medal_psychic_200"
        ],
        outputDir: MEDALS_DIR
    },
    // 2. Epic Medals (3x3)
    {
        imagePath: "C:\\Users\\lucas\\.gemini\\antigravity\\brain\\fa8f899c-4945-49f1-8fc7-a96784c2067f\\medals_epic_grid_1780688411405.png",
        cols: 3,
        rows: 3,
        names: [
            "medal_legend", "medal_champion", "medal_board_legend",
            "medal_collector_legend", "medal_league_champion", "medal_colosseum_legend",
            "medal_perfect_1", "medal_comeback_1", "medal_fair_play_legend"
        ],
        outputDir: MEDALS_DIR
    },
    // 3. Flat Avatars (4x4)
    {
        imagePath: "C:\\Users\\lucas\\.gemini\\antigravity\\brain\\fa8f899c-4945-49f1-8fc7-a96784c206
<truncated 2898 bytes>