import eslint from '@eslint/js';
import tsPlugin from '@typescript-eslint/eslint-plugin';
import tsParser from '@typescript-eslint/parser';
import effectPlacementPlugin from './src/app/rules/index.js';

export default [
  { ignores: ['node_modules/**', 'dist/**'] },
  ...tsPlugin.configs['flat/recommended'],
  {
    files: ['**/*.ts'],
    languageOptions: {
      parser: tsParser,
    },
    rules: {
      '@typescript-eslint/no-explicit-any': 'off',
      '@typescript-eslint/no-unused-vars': 'off',
    },
  },
  {
    files: ['**/*.ts'],
    plugins: {
      'effect-placement': effectPlacementPlugin,
    },
    rules: {
      'effect-placement/no-effect-outside-constructor': 'error',
    },
  },
];
