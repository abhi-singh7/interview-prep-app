/**
 * Webpack configuration for handling Monaco Editor font files (.ttf)
 * 
 * This configuration adds asset handling rules for .ttf, .woff, .woff2 fonts
 * that Monaco Editor references in its CSS for icon support (codicons).
 */

module.exports = {
  module: {
    rules: [
      {
        test: /\.ttf$/,
        type: 'asset/resource',
        generator: {
          filename: 'assets/fonts/[name][hash][ext]'
        }
      },
      {
        test: /\.(woff|woff2|eot|otf)$/,
        type: 'asset/resource'
      }
    ]
  }
};
