import toolUtil from './toolUtil'
import menuService from '@/api/menu'
import type { MenuItem as ApiMenuItem } from '@/api/menu'

// 前端菜单项类型
interface MenuItem {
	roleName: string
	backMenu?: Array<{
		child?: Array<{
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
	 * 从后端获取用户菜单
	 */
	async fetchUserMenus(): Promise<MenuItem[]> {
		try {
			// 调用 /menus/auth 接口获取当前用户角色的菜单
			const apiMenus = await menuService.getAuthMenus()
			
			// 将 API 菜单数据转换为前端需要的格式
			const menus = this.convertApiMenusToFrontend(apiMenus)
			
			// 存储到本地
			toolUtil.storageSet('menus', JSON.stringify(menus))
			
			return menus
		} catch (error) {
			console.error('获取用户菜单失败:', error)
			throw error
		}
	},

	/**
	 * 将 API 菜单数据转换为前端格式
	 * @param apiMenus API 返回的菜单数据
	 */
	convertApiMenusToFrontend(apiMenus: ApiMenuItem[]): MenuItem[] {
		// 按 parentId 分组菜单
		const parentMenus = apiMenus.filter(m => m.parentId === null)
		const childMenus = apiMenus.filter(m => m.parentId !== null)
		
		// 构建前端菜单结构
		return parentMenus.map(parent => {
			const menuItem: MenuItem = {
				roleName: '',
				backMenu: []
			}
			
			// 找到该父菜单下的所有子菜单
			const children = childMenus.filter(c => c.parentId === parent.id)
			
			if (children.length > 0) {
				menuItem.backMenu = [{
					child: children.map(child => ({
						menuJump: child.path || child.component || '',
						buttons: child.buttons || []
					}))
				}]
			}
			
			return menuItem
		})
	},

	/**
	 * 清除缓存的菜单数据
	 */
	clearCache(): void {
		toolUtil.storageRemove('menus')
	}
}

export default menu