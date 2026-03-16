import toolUtil from './toolUtil'

// 根据你实际的数据结构定义菜单项类型
interface MenuItem {
	roleName: string
	backMenu?: Array<{
		child?: Array<{
			tableName: string
			menuJump?: string
			buttons: string[]
		}>
	}>
}

const menu = {
	list(): MenuItem[] | null {
		const menus = toolUtil.storageGet('menus')
		if (menus) {
			return JSON.parse(menus) as MenuItem[]
		} else {
			return null
		}
	},
	
	/**
	 * 保存菜单数据
	 * @param menuData - 菜单数据数组
	 */
	save(menuData: MenuItem[]): void {
		toolUtil.storageSet('menus', JSON.stringify(menuData))
	},
	
	/**
	 * 清空菜单数据
	 */
	clear(): void {
		toolUtil.storageRemove('menus')
	}
}

export default menu