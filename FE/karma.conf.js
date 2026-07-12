module.exports = function (config) {
  config.set({
    frameworks: ['jasmine'],
    plugins: [
      require('karma-jasmine'),
      require('karma-chrome-launcher'),
      require('karma-jasmine-html-reporter'),
      require('karma-coverage'),
    ],
    client: {
      jasmine: {},
      clearContext: false,
    },
    reporters: ['progress', 'kjhtml', 'coverage'],
    coverageReporter: {
      dir: require('path').join(__dirname, './coverage/FE'),
      subdir: '.',
      reporters: [{ type: 'html' }, { type: 'text-summary' }, { type: 'lcovonly' }],
      // Honest floor at the current measured baseline (64.66%/43.25%/67.59%/66.69%
      // statements/branches/functions/lines, minus margin), not an aspirational
      // number. Raised from the original 35/18/32/37 once all 18 previously-untested
      // components got real tests. Raise again as coverage grows further.
      check: {
        global: {
          statements: 60,
          branches: 38,
          functions: 62,
          lines: 62,
        },
      },
    },
    browsers: ['Chrome'],
    restartOnFileChange: true,
  });
};
