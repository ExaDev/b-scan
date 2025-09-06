module.exports = {
  root: true,
  extends: '@react-native',
  overrides: [
    {
      files: ['jest.setup.js'],
      env: {
        jest: true,
      },
    },
  ],
};
