// vue.config.ts
const path = require('path')

// 路径解析函数
function resolve(dir) {
	return path.join(__dirname, dir)
}

// 动态设置公共路径
function getPublicPath() {
	if (process.env.NODE_ENV === 'production') {
		return '././'
	} else {
		return '/'
	}
}

module.exports = {
	// 公共路径配置
	publicPath: getPublicPath(),

	// 页面入口配置 - 确保指向 main.ts
	pages: {
		index: {
			entry: 'src/main.ts',        // 主入口文件
			template: 'public/index.html', // HTML 模板
			filename: 'index.html',       // 输出文件名
			title: '中文社区交流平台',      // 页面标题
			chunks: ['chunk-vendors', 'chunk-common', 'index']
		}
	},

	// webpack 配置
	configureWebpack: {
		resolve: {
			fallback: {
				path: require.resolve('path-browserify')
			},
		},
		plugins: [
			new (require('webpack')).DefinePlugin({
				__VUE_PROD_HYDRATION_MISMATCH_DETAILS__: JSON.stringify(true)
			})
		]
	},

	// 关闭 ESLint 检查
	lintOnSave: false,

	// 开发服务器配置
	devServer: {
		host: '0.0.0.0',     // 监听所有地址
		port: 8082,          // 开发端口
		hot: true,           // 开启热更新
		https: false,        // 不使用 HTTPS
		open: false,         // 不自动打开浏览器
		proxy: {             // API 代理配置
			'/api': {
				target: 'http://localhost:8080',
				changeOrigin: true,
				secure: false,
				pathRewrite: {
					'^/api': ''
				}
			}
		}
	},

	// webpack 链式配置
	chainWebpack(config) {
		// SVG 图标处理配置
		config.module
			.rule('svg')
			.exclude.add(resolve('src/icons'))
			.end()

		config.module
			.rule('icons')
			.test(/\.svg$/)
			.include.add(resolve('src/icons'))
			.end()
			.use('svg-sprite-loader')
			.loader('svg-sprite-loader')
			.options({
				symbolId: 'icon-[name]'
			})
			.end()
	}
}
