const webpack = require('webpack');

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
        test: /\.(woff|woff2|eot|otf)$/i,
        type: 'asset/resource'
      }
    ]
  }
};
