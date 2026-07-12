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
      // Honest floor at the current measured baseline (38.45%/20.71%/35.61%/40.43%
      // statements/branches/functions/lines), not an aspirational number - this repo
      // has 18 of 22 components with zero unit tests. Raise these as that gap closes.
      check: {
        global: {
          statements: 35,
          branches: 18,
          functions: 32,
          lines: 37,
        },
      },
    },
    browsers: ['Chrome'],
    restartOnFileChange: true,
  });
};
