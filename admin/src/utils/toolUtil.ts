import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import menu from './menu'
import CryptoJS from 'crypto-js'

// 定义密钥和向量（可根据实际配置提取到环境变量）
const KEY = '1234567890123456'
const IV = 'abcdefghijklmnop'

interface ToolUtil {
	message(msg: string, type: 'success' | 'warning' | 'info' | 'error', close?: () => void): void
	notify(title: string, msg: string, type?: 'success' | 'warning' | 'info' | 'error', close?: () => void): void
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
	isAuth(tableName: string, key: string): boolean
	getCurDateTime(): string
	getCurDate(): string
	encryptDes(message: string): string
	decryptDes(ciphertext: string): string
	encryptAes(msg: string): string
	decryptAes(msg: string): string
}

const toolUtil: ToolUtil = {
	// 提示语
	message(msg, type, close?) {
		ElMessage({
			message: msg,
			type: type,
			duration: 2500,
			onClose() {
				if (close) {
					close()
				}
			}
		})
	},

	// 右部提示语
	notify(title, msg, type = 'success', close?) {
		ElNotification({
			title: title,
			message: msg,
			type: type,
			onClose() {
				if (close) {
					close()
				}
			}
		})
	},

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
		localStorage.removeItem('sessionTable')
		localStorage.removeItem('adminName')
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
	 * 是否有权限
	 * @param tableName 表名（可能包含跳转类型，如 "tiezi/list"）
	 * @param key 按钮权限标识
	 */
	isAuth(tableName: string, key: string): boolean {
		let role = toolUtil.storageGet('role')
		if (!role) {
			role = '管理员'
		}
		const menus = menu.list()
		
		// 添加类型检查确保 menus 存在且为数组
		if (Array.isArray(menus) && menus.length > 0) {
			for (let i = 0; i < menus.length; i++) {
				// 添加类型检查确保 roleName 存在
				if (menus[i]?.roleName === role) {
					// 添加类型检查确保 backMenu 存在且为数组
					const backMenu = menus[i]?.backMenu
					if (Array.isArray(backMenu) && backMenu.length > 0) {
						for (let j = 0; j < backMenu.length; j++) {
							// 添加类型检查确保 child 存在且为数组
							const child = backMenu[j]?.child
							if (Array.isArray(child) && child.length > 0) {
								for (let k = 0; k < child.length; k++) {
									const currentItem = child[k]
									// 添加类型检查确保必要属性存在
									if (!currentItem?.tableName || !currentItem?.buttons) {
										continue
									}
									
									if (tableName.split('/').length > 1) {
										// 处理带跳转类型的表名
										if (tableName.split('/')[0] === currentItem.tableName &&
											tableName.split('/')[1] === currentItem.menuJump) {
											const buttons = currentItem.buttons.join(',')
											return buttons.indexOf(key) !== -1 || false
										}
									} else {
										// 处理普通表名
										if (tableName === 'orders' && currentItem.tableName === 'orders' && !currentItem.menuJump) {
											const buttons = currentItem.buttons.join(',')
											return buttons.indexOf(key) !== -1 || false
										}
										if (tableName !== 'orders' && tableName === currentItem.tableName) {
											const buttons = currentItem.buttons.join(',')
											return buttons.indexOf(key) !== -1 || false
										}
									}
								}
							}
						}
					}
				}
			}
		}
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