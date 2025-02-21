const airbnb = require("@neutrinojs/airbnb");
const react = require("@neutrinojs/react");
const mocha = require("@neutrinojs/mocha");
const xtraplatform = require("@xtraplatform/neutrino");
const path = require("path");
const fs = require("fs");

const mains = {};
fs.readdirSync(path.join(__dirname, "src/apps")).forEach(
  (app) =>
    (mains[app === "common" ? "ignore" : app] = {
      name: app,
      entry: `apps/${app}/index`,
      filename: `templates/app-${app}.mustache`,
      template: "mustache.ejs",
      minify: false,
      inject: false,
      scriptLoading: "defer",
      publicPath: "{{urlPrefix}}/ogcapi-html",
    })
);
fs.readdirSync(path.join(__dirname, "src/styles")).forEach(
  (style) =>
    (mains[`style-${style}`] = {
      name: `style-${style}`,
      entry: `styles/${style}/index`,
      filename: `templates/style-${style}.mustache`,
      template: "mustache.ejs",
      minify: false,
      inject: false,
      scriptLoading: "defer",
      publicPath: "{{urlPrefix}}/ogcapi-html",
      templateParameters: (compilation, assets, assetTags, options) => {
        const favicon = Object.keys(compilation.assets).find(
          (key) => key.indexOf("assets/favicon.") === 0
        );
        const files = favicon
          ? { ...assets, favicon: `${options.publicPath}/${favicon}` }
          : assets;

        return {
          compilation: compilation,
          webpackConfig: compilation.options,
          htmlWebpackPlugin: {
            tags: assetTags,
            files: files,
            options: options,
          },
        };
      },
    })
);

module.exports = {
  options: {
    root: __dirname,
    output: "../../../build/generated/src/main/resources/de/ii/ogcapi/html",
    mains: mains,
  },
  use: [
    airbnb(),
    react(),
    mocha(),
    xtraplatform({
      lib: false,
      modulePrefixes: ["ogcapi", "@ogcapi"],
    }),
    (neutrino) => {
      neutrino.config.optimization.merge({
        runtimeChunk: {
          name: "common",
        },
        splitChunks: {
          chunks: "all",
          name: true,
          cacheGroups: {
            default: false,
            vendors: false,
            defaultVendors: false,
            common: {
              name: "common",
              minChunks: 2,
            },
          },
        },
      });

      neutrino.config.module
        .rule("font")
        .test(/\.(eot|ttf|woff|woff2|ico)(\?v=\d+\.\d+\.\d+)?$/);

      neutrino.config.performance
        .maxEntrypointSize(2048000)
        .maxAssetSize(1024000);

      //for cesium/c137
      neutrino.config.module
        .rule("compile")
        .use("babel")
        .tap((options) => ({
          ...options,
          plugins: [
            ...options.plugins,
            "@babel/plugin-proposal-nullish-coalescing-operator",
          ],
        }));
      neutrino.config.module
        .rule("compile")
        .include.add(new RegExp(`^.*?\\/\\.yarn\\/cache\\/c137.*?$`)); //TODO: add cache in addition to $$virtual in @xtraplatform/neutrino, use modulePrefixes
    },
  ],
};
