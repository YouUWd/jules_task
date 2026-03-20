#!/usr/bin/env node

/**
 * 部署文档转 PDF 工具
 *
 * 该脚本用于将 Markdown 文件导出为 PDF，且在导出前自动调用 generate-image.js
 * 提取并渲染所有包含的 Mermaid 代码块为高清晰度 PNG 图片。
 *
 * 依赖安装说明:
 * 运行此脚本前，请确保您已全局安装了 Node.js 及以下 npm 包：
 * npm install -g md-to-pdf
 *
 * 用法:
 * node generate-docs.js <输入文件.md> [图像缩放比例]
 */

const fs = require('fs');
const cp = require('child_process');
const path = require('path');

// 提取命令行参数
const args = process.argv.slice(2);

if (args.length < 1) {
    console.error('[ERROR] 缺少必要的参数: 输入文件.md');
    console.error('用法: node generate-docs.js <输入文件.md> [图像缩放比例]');
    process.exit(1);
}

const inputFile = args[0];
const parsedPath = path.parse(inputFile);
const baseName = parsedPath.name; // 例如: "Deployment"
const scaleFactor = args.length > 1 ? args[1] : 3;

// 配置文件与路径
const outputFilePDF = `${baseName}.pdf`;
const tempMarkdownFile = `temp_${baseName}.md`;

if (!fs.existsSync(inputFile)) {
    console.error(`[ERROR] 找不到输入文件: ${inputFile}`);
    process.exit(1);
}

console.log(`[INFO] 开始使用 generate-image 生成高清图片并获取内容...`);

try {
    // 1. 调用 generate-image 脚本，捕获其标准输出以获得替换好的 Markdown
    const markdownContent = cp.execSync(`node generate-image.js ${inputFile} ${scaleFactor}`, {
        encoding: 'utf-8',
        stdio: ['inherit', 'pipe', 'inherit']
    });

    console.log('[INFO] 开始生成 PDF...');

    // 2. 将替换过图片链接的新内容写入临时 Markdown 文件
    fs.writeFileSync(tempMarkdownFile, markdownContent);

    // 3. 使用 md-to-pdf 工具将临时 Markdown 文件转换为 PDF
    cp.execSync(`md-to-pdf ${tempMarkdownFile}`, { stdio: 'inherit' });

    // md-to-pdf 默认会生成与输入文件同名但后缀为 .pdf 的文件
    const generatedTempPDF = tempMarkdownFile.replace('.md', '.pdf');

    // 4. 重命名为最终的输出文件名
    if (fs.existsSync(generatedTempPDF)) {
        fs.renameSync(generatedTempPDF, outputFilePDF);
        console.log(`[SUCCESS] PDF 生成成功: ${outputFilePDF}`);
    } else {
        console.warn(`[WARNING] 未找到预期的临时 PDF 文件: ${generatedTempPDF}`);
    }
} catch (error) {
     console.error(`[ERROR] 文档转换流程时发生错误: ${error.message}`);
} finally {
    // 5. 自动清理临时的 Markdown 文件
    if (fs.existsSync(tempMarkdownFile)) {
        fs.unlinkSync(tempMarkdownFile);
    }
}

console.log('[INFO] 文档转换流程全部完成！');
