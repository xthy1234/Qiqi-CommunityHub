/* eslint-disable */
import { saveAs } from 'file-saver'
import * as XLSX from 'xlsx'

/**
 * 表格范围接口
 */
interface Range {
  s: { r: number; c: number } // 开始位置
  e: { r: number; c: number } // 结束位置
}

/**
 * 单元格数据接口
 */
interface CellData {
  v: any // 单元格值
  t?: 'n' | 'b' | 's' // 单元格类型
  z?: string // 格式
}

/**
 * 工作簿接口
 */
interface Workbook {
  SheetNames: string[]
  Sheets: { [sheet: string]: any }
}

/**
 * 列宽配置接口
 */
interface ColumnWidth {
  wch: number
}

/**
 * 从 HTML 表格生成数组数据
 * @param table HTML 表格元素
 * @returns [数据数组, 合并范围数组]
 */
function generateArrayFromTable(table: HTMLElement): [any[][], Range[]] {
  const out: any[][] = []
  const rows = table.querySelectorAll('tr')
  const ranges: Range[] = []
  
  for (let rowIndex = 0; rowIndex < rows.length; ++rowIndex) {
    const outRow: any[] = []
    const row = rows[rowIndex]
    const columns = row.querySelectorAll('td')
    
    for (let colIndex = 0; colIndex < columns.length; ++colIndex) {
      const cell = columns[colIndex]
      const colspan = cell.getAttribute('colspan')
      const rowspan = cell.getAttribute('rowspan')
      let cellValue: any = cell.innerText
      
      if (cellValue !== "" && !isNaN(Number(cellValue))) {
        cellValue = Number(cellValue)
      }

      // 跳过已存在的范围
      ranges.forEach(function (range) {
        if (rowIndex >= range.s.r && rowIndex <= range.e.r && 
            outRow.length >= range.s.c && outRow.length <= range.e.c) {
          for (let i = 0; i <= range.e.c - range.s.c; ++i) {
            outRow.push(null)
          }
        }
      })

      // 处理行合并
      if (rowspan || colspan) {
        const actualRowspan = Number(rowspan) || 1
        const actualColspan = Number(colspan) || 1
        ranges.push({
          s: {
            r: rowIndex,
            c: outRow.length
          },
          e: {
            r: rowIndex + actualRowspan - 1,
            c: outRow.length + actualColspan - 1
          }
        })
      }

      // 处理单元格值
      outRow.push(cellValue !== "" ? cellValue : null)

      // 处理列合并
      if (colspan) {
        const actualColspan = Number(colspan)
        for (let k = 0; k < actualColspan - 1; ++k) {
          outRow.push(null)
        }
      }
    }
    out.push(outRow)
  }
  return [out, ranges]
}

/**
 * 日期转换为数字
 * @param date 日期对象或字符串
 * @param date1904 是否使用1904日期系统
 * @returns 数字格式的日期
 */
function convertDateToNumber(date: Date | string, date1904: boolean = false): number {
  if (date1904) {
    // @ts-ignore
    date = new Date(+date + 1462 * 24 * 60 * 60 * 1000)
  }
  const epoch = Date.parse(date as string)
  return (epoch - new Date(Date.UTC(1899, 11, 30)).getTime()) / (24 * 60 * 60 * 1000)
}

/**
 * 从二维数组创建工作表
 * @param data 二维数组数据
 * @param opts 选项配置
 * @returns 工作表对象
 */
function createWorksheetFromArray(data: any[][], opts?: any): any {
  const ws: any = {}
  const range = {
    s: { c: 10000000, r: 10000000 },
    e: { c: 0, r: 0 }
  }
  
  for (let rowIndex = 0; rowIndex < data.length; ++rowIndex) {
    for (let colIndex = 0; colIndex < data[rowIndex].length; ++colIndex) {
      if (range.s.r > rowIndex) range.s.r = rowIndex
      if (range.s.c > colIndex) range.s.c = colIndex
      if (range.e.r < rowIndex) range.e.r = rowIndex
      if (range.e.c < colIndex) range.e.c = colIndex
      
      const cell: CellData = {
        v: data[rowIndex][colIndex]
      }
      
      if (cell.v == null) continue
      const cellRef = XLSX.utils.encode_cell({ c: colIndex, r: rowIndex })

      if (typeof cell.v === 'number') {
        cell.t = 'n'
      } else if (typeof cell.v === 'boolean') {
        cell.t = 'b'
      } else if (cell.v instanceof Date) {
        cell.t = 'n'
        cell.z = XLSX.SSF._table[14]
        cell.v = convertDateToNumber(cell.v)
      } else {
        cell.t = 's'
      }

      ws[cellRef] = cell
    }
  }
  
  if (range.s.c < 10000000) {
    ws['!ref'] = XLSX.utils.encode_range(range)
  }
  return ws
}

/**
 * 工作簿构造函数
 */
class ExcelWorkbook implements Workbook {
  SheetNames: string[] = []
  Sheets: { [sheet: string]: any } = {}

  constructor() {
    if (!(this instanceof ExcelWorkbook)) {
      return new ExcelWorkbook()
    }
  }
}

/**
 * 字符串转 ArrayBuffer
 * @param str 输入字符串
 * @returns ArrayBuffer 对象
 */
function stringToArrayBuffer(str: string): ArrayBuffer {
  const buf = new ArrayBuffer(str.length)
  const view = new Uint8Array(buf)
  for (let i = 0; i < str.length; ++i) {
    view[i] = str.charCodeAt(i) & 0xFF
  }
  return buf
}

/**
 * 导出 HTML 表格到 Excel
 * @param tableId 表格元素 ID
 */
export function exportTableToExcel(tableId: string): void {
  const tableElement = document.getElementById(tableId)
  if (!tableElement) {
    throw new Error(`找不到 ID 为 ${tableId} 的表格元素`)
  }
  
  const [dataArray, mergeRanges] = generateArrayFromTable(tableElement)
  const worksheetName = "SheetJS"
  
  const workbook = new ExcelWorkbook()
  const worksheet = createWorksheetFromArray(dataArray)
  
  // 添加合并范围到工作表
  worksheet['!merges'] = mergeRanges
  
  // 添加工作表到工作簿
  workbook.SheetNames.push(worksheetName)
  workbook.Sheets[worksheetName] = worksheet
  
  const workbookOut = XLSX.write(workbook, {
    bookType: 'xlsx',
    bookSST: false,
    type: 'binary'
  })
  
  saveAs(new Blob([stringToArrayBuffer(workbookOut)], {
    type: "application/octet-stream"
  }), "export.xlsx")
}

/**
 * 导出 JSON 数据到 Excel 的配置接口
 */
interface ExportJsonOptions {
  multiHeader?: string[][]
  header: string[]
  data: any[][]
  filename?: string
  merges?: string[]
  autoWidth?: boolean
  bookType?: XLSX.BookType
}

/**
 * 导出 JSON 数据到 Excel
 * @param options 导出配置选项
 */
export function exportJsonToExcel({
  multiHeader = [],
  header,
  data,
  filename = 'excel-list',
  merges = [],
  autoWidth = true,
  bookType = 'xlsx'
}: ExportJsonOptions): void {
  // 准备数据
  const exportData = [...data]
  exportData.unshift(header)
  
  // 添加多级表头
  for (let i = multiHeader.length - 1; i > -1; i--) {
    exportData.unshift(multiHeader[i])
  }
  
  const worksheetName = "SheetJS"
  const workbook = new ExcelWorkbook()
  const worksheet = createWorksheetFromArray(exportData)
  
  // 处理合并单元格
  if (merges.length > 0) {
    if (!worksheet['!merges']) worksheet['!merges'] = []
    merges.forEach(item => {
      worksheet['!merges'].push(XLSX.utils.decode_range(item))
    })
  }
  
  // 自动调整列宽
  if (autoWidth) {
    const columnWidths = exportData.map(row => 
      row.map(val => {
        // 判断是否为 null/undefined
        if (val == null) {
          return { wch: 10 }
        }
        // 判断是否包含中文字符
        else if (String(val).charCodeAt(0) > 255) {
          return { wch: String(val).length * 2 }
        } else {
          return { wch: String(val).length }
        }
      })
    )
    
    // 以第一行为基准计算最大宽度
    let maxWidths = columnWidths[0]
    for (let i = 1; i < columnWidths.length; i++) {
      for (let j = 0; j < columnWidths[i].length; j++) {
        if (maxWidths[j].wch < columnWidths[i][j].wch) {
          maxWidths[j].wch = columnWidths[i][j].wch
        }
      }
    }
    worksheet['!cols'] = maxWidths
  }
  
  // 添加工作表到工作簿
  workbook.SheetNames.push(worksheetName)
  workbook.Sheets[worksheetName] = worksheet
  
  const workbookOut = XLSX.write(workbook, {
    bookType: bookType,
    bookSST: false,
    type: 'binary'
  })
  
  saveAs(new Blob([stringToArrayBuffer(workbookOut)], {
    type: "application/octet-stream"
  }), `${filename}.${bookType}`)
}

/**
 * 格式化 JSON 数据
 * @param jsonData 原始 JSON 数据
 * @param fieldFilter 字段过滤器数组
 * @returns 格式化后的二维数组
 */
function formatJsonData(jsonData: any[], fieldFilter: string[]): any[][] {
  return jsonData.map(item => 
    fieldFilter.map(field => item[field])
  )
}

/**
 * 导出 JSON 数据到 Excel（简化版）
 * @param headers 表头数组
 * @param jsonData 原始 JSON 数据
 * @param fieldFilter 字段过滤器
 * @param fileName 文件名
 */
export function exportJsonToExcelSimple(
  headers: string[],
  jsonData: any[],
  fieldFilter: string[],
  fileName: string
): void {
  const formattedData = formatJsonData(jsonData, fieldFilter)
  exportJsonToExcel({
    header: headers,
    data: formattedData,
    filename: fileName
  })
}