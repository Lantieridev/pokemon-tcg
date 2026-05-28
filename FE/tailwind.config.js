/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{html,ts}",
  ],
  theme: {
    extend: {
      colors: {
        ink:    '#f4f1ff',
        'ink-dim':'#a59cc4',
        'ink-faint':'#5d557d',
        gold:   '#e3c878',
        'gold-bright':'#fff2c2',
        felt:   '#0e0d1a',
        'felt-2': '#15132a',
        'felt-3': '#1d1838',
        'fire':      '#ff7a3d',
        'water':     '#4aa3ff',
        'grass':     '#5ad27a',
        'lightning': '#ffcc33',
        'psychic':   '#c87bff',
        'fighting':  '#d97d4a',
      },
      fontFamily: {
        display: ['"Bowlby One"','"Russo One"','sans-serif'],
        chunky:  ['"Russo One"','"Bowlby One"','sans-serif'],
        sans:    ['Nunito','system-ui','sans-serif'],
        mono:    ['"JetBrains Mono"','monospace'],
      },
    },
  },
  plugins: [],
};
