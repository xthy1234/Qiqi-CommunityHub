import menu from './menu'
import CryptoJS from 'crypto-js'

// 定义密钥和向量（可根据实际配置提取到环境变量）
const KEY = '1234567890123456'
const IV = 'abcdefghijklmnop'

interface ToolUtil {
	storageSet(key: string, value: string): void
	storageGet(key: string): string
	storageGetObj<T = any>(key: string): T | null
	storageRemove(key: string): void
	storageClear(): void
	isEmail(s: string): boolean
	isMobile(s: string): boolean
	isPhone(s: string): boolean
	isURL(s: string): boolean
	isNumber(s: string): boolean
	isIntNumer(s: string): boolean
	checkIdCard(idcard: string): boolean

	getCurDateTime(): string
	getCurDate(): string
	encryptDes(message: string): string
	decryptDes(ciphertext: string): string
	encryptAes(msg: string): string
	decryptAes(msg: string): string
	hasAuth(routePath: string, actionType?: string): boolean
}

const toolUtil: ToolUtil = {

	storageSet(key, value) {
		localStorage.setItem(key, value)
	},

	storageGet(key) {
		return localStorage.getItem(key) ? localStorage.getItem(key) as string : ''
	},

	storageGetObj(key) {
		const item = localStorage.getItem(key)
		return item ? JSON.parse(item) : null
	},

	storageRemove(key) {
		localStorage.removeItem(key)
	},

	storageClear() {
		localStorage.removeItem('Token')
		localStorage.removeItem('role')
		localStorage.removeItem('frontSessionTable')
		localStorage.removeItem('nickname')
		localStorage.removeItem('avatar')
		localStorage.removeItem('userid')
		localStorage.removeItem('roleId')
		localStorage.removeItem('UserInfo')
		localStorage.removeItem('menus')
	},

	/**
	 * 邮箱验证
	 */
	isEmail(s) {
		return /^([a-zA-Z0-9_-])+@([a-zA-Z0-9_-])+((.[a-zA-Z0-9_-]{2,3}){1,2})$/.test(s)
	},

	/**
	 * 手机号码验证
	 */
	isMobile(s) {
		return /^(0|86|17951)?(13[0-9]|15[012356789]|16[6]|19[89]|17[01345678]|18[0-9]|14[579])[0-9]{8}$/.test(s)
	},

	/**
	 * 电话号码验证
	 */
	isPhone(s) {
		return /^([0-9]{3,4}-)?[0-9]{7,8}$/.test(s)
	},

	/**
	 * URL地址验证
	 */
	isURL(s) {
		return /^http[s]?:\/\/.*/.test(s)
	},

	/**
	 * 匹配数字（可为空）
	 */
	isNumber(s) {
		return /(^-?[+-]?([0-9]*\.?[0-9]+|[0-9]+\.?[0-9]*)([eE][+-]?[0-9]+)?$)|(^$)/.test(s)
	},

	/**
	 * 匹配整数（可为空）
	 */
	isIntNumer(s) {
		return /(^-?\d+$)|(^$)/.test(s)
	},

	/**
	 * 身份证校验
	 */
	checkIdCard(idcard) {
		const regIdCard = /(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/
		return regIdCard.test(idcard)
	},

	/**
	 * 是否有权限访问指定路由
	 * @param routePath 路由路径，如 '/articleList' 或 '/index/articleList'
	 * @param actionType 操作类型（可选），如 'add', 'edit', 'delete', 'query'
	 */
	hasAuth(routePath: string, actionType?: string) {
		const roleId = toolUtil.storageGet('roleId')
		const menus = menu.list()
		
		// 如果没有菜单数据，默认允许访问（或可根据需求改为拒绝）
		if (!menus || menus.length === 0) {
			return true
		}
		
		// 遍历所有角色的菜单
		for (const roleMenu of menus) {
			const backMenu = roleMenu?.backMenu
			if (!Array.isArray(backMenu) || backMenu.length === 0) {
				continue
			}
			
			// 遍历所有一级菜单
			for (const level1Menu of backMenu) {
				const child = level1Menu?.child
				if (!Array.isArray(child) || child.length === 0) {
					continue
				}
				
				// 遍历二级菜单项
				for (const menuItem of child) {
					if (!menuItem) {continue}
					
					// 检查路由路径是否匹配
					const menuJump = menuItem.menuJump || ''
					if (menuJump && menuJump.includes(routePath)) {
						// 如果没有指定操作类型，直接返回有权限
						if (!actionType) {
							return true
						}
						
						// 检查按钮权限
						const buttons = menuItem.buttons || []
						if (buttons.includes(actionType)) {
							return true
						}
					}
				}
			}
		}
		
		// 如果都没有匹配，返回无权限
		return false
	},

	/**
	 * 获取当前时间（yyyy-MM-dd hh:mm:ss）
	 */
	getCurDateTime() {
		const currentTime = new Date()
		const year = currentTime.getFullYear()
		const month = currentTime.getMonth() + 1 < 10 ? '0' + (currentTime.getMonth() + 1) : currentTime.getMonth() + 1
		const day = currentTime.getDate() < 10 ? '0' + currentTime.getDate() : currentTime.getDate()
		const hour = currentTime.getHours() < 10 ? '0' + currentTime.getHours() : currentTime.getHours()
		const minute = currentTime.getMinutes() < 10 ? '0' + currentTime.getMinutes() : currentTime.getMinutes()
		const second = currentTime.getSeconds() < 10 ? '0' + currentTime.getSeconds() : currentTime.getSeconds()
		return `${year}-${month}-${day} ${hour}:${minute}:${second}`
	},

	/**
	 * 获取当前日期（yyyy-MM-dd）
	 */
	getCurDate() {
		const currentTime = new Date()
		const year = currentTime.getFullYear()
		const month = currentTime.getMonth() + 1 < 10 ? '0' + (currentTime.getMonth() + 1) : currentTime.getMonth() + 1
		const day = currentTime.getDate() < 10 ? '0' + currentTime.getDate() : currentTime.getDate()
		return `${year}-${month}-${day}`
	},

	// DES 加密（ECB模式，PKCS7填充）
	encryptDes(message) {
		const keyHex = CryptoJS.enc.Utf8.parse(KEY)
		const encrypted = CryptoJS.DES.encrypt(message, keyHex, {
			mode: CryptoJS.mode.ECB,
			padding: CryptoJS.pad.Pkcs7
		})
		return encrypted.toString()
	},

	// DES 解密
	decryptDes(ciphertext) {
		const keyHex = CryptoJS.enc.Utf8.parse(KEY)
		const decrypted = CryptoJS.DES.decrypt(ciphertext, keyHex, {
			mode: CryptoJS.mode.ECB,
			padding: CryptoJS.pad.Pkcs7
		})
		return decrypted.toString(CryptoJS.enc.Utf8)
	},

	// AES 加密（CBC模式，PKCS7填充）
	encryptAes(msg) {
		const key = CryptoJS.enc.Utf8.parse(KEY)
		const iv = CryptoJS.enc.Utf8.parse(IV)
		const encrypted = CryptoJS.AES.encrypt(msg, key, {
			mode: CryptoJS.mode.CBC,
			padding: CryptoJS.pad.Pkcs7,
			iv: iv
		})
		return encrypted.toString()
	},

	// AES 解密
	decryptAes(msg) {
		const key = CryptoJS.enc.Utf8.parse(KEY)
		const iv = CryptoJS.enc.Utf8.parse(IV)
		const bytes = CryptoJS.AES.decrypt(msg, key, {
			mode: CryptoJS.mode.CBC,
			padding: CryptoJS.pad.Pkcs7,
			iv: iv
		})
		return bytes.toString(CryptoJS.enc.Utf8)
	}
}

export default toolUtil
