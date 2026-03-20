#!/usr/bin/env node

/**
 * 部署文档转换工具
 *
 * 该脚本用于将 Deployment.md 文件中包含的 Mermaid 代码块：
 * 1. 单独渲染为高清晰度的 PNG 图片 (Deployment-Mermaid-*.png)
 * 2. 生成能够正确渲染上述图片的 PDF 文档 (Deployment.pdf)
 *
 * 依赖安装说明:
 * 运行此脚本前，请确保您已全局安装了 Node.js 及以下 npm 包：
 * npm install -g @mermaid-js/mermaid-cli md-to-pdf
 *
 * 用法:
 * node generate-docs.js <输入文件.md> [图像缩放比例]
 *
 * 图像质量控制:
 * 可选的第二个参数 [图像缩放比例] 控制生成图片的设备像素比 (device-scale-factor)。
 * 默认值为 3。增大此数值（如 4 或 5）可以提高生成的 Mermaid 图表的清晰度和质量。
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
const scaleFactor = args.length > 1 ? parseInt(args[1], 10) : 3;

if (isNaN(scaleFactor) || scaleFactor < 1) {
    console.error('[ERROR] 图像缩放比例必须是一个大于或等于 1 的整数。');
    process.exit(1);
}

// 配置文件与路径
const outputFilePDF = `${baseName}.pdf`;
const tempMarkdownFile = `temp_${baseName}.md`;

console.log(`[INFO] 开始读取文档: ${inputFile}`);
console.log(`[INFO] 图像质量缩放比例设置为: ${scaleFactor}`);

if (!fs.existsSync(inputFile)) {
    console.error(`[ERROR] 找不到输入文件: ${inputFile}`);
    process.exit(1);
}

const content = fs.readFileSync(inputFile, 'utf-8');
const mermaidRegex = /```mermaid([\s\S]*?)```/g;
let match;
let count = 1;
let newContent = content;

console.log('[INFO] 开始提取并渲染 Mermaid 图表...');

// 遍历所有的 mermaid 代码块
while ((match = mermaidRegex.exec(content)) !== null) {
    const mmContent = match[1].trim();
    const tempFile = `temp_mermaid_${count}.mmd`;
    const finalImage = `${baseName}-Mermaid-${count}.png`;

    // 1. 将 mermaid 代码块保存到临时 .mmd 文件
    fs.writeFileSync(tempFile, mmContent);
    console.log(`[INFO] 正在生成图片: ${finalImage}`);

    try {
        // 使用 mmdc 渲染为 PNG 图片。通过 -s 参数控制 device scale factor (图像质量)
        cp.execSync(`mmdc -i ${tempFile} -o ${finalImage} -s ${scaleFactor} -b white`, { stdio: 'inherit' });

        // 2. 将原 markdown 中的代码块替换为刚才生成的图片链接
        newContent = newContent.replace(match[0], `![Mermaid 图表 ${count}](${finalImage})`);
    } catch (error) {
         console.error(`[ERROR] 生成图片 ${finalImage} 时发生错误: ${error.message}`);
    } finally {
        // 清理临时 .mmd 文件
        if (fs.existsSync(tempFile)) {
            fs.unlinkSync(tempFile);
        }
    }
    count++;
}

console.log('[INFO] Mermaid 渲染完成。开始生成 PDF...');

// 3. 将替换过图片链接的新内容写入临时 Markdown 文件
fs.writeFileSync(tempMarkdownFile, newContent);

try {
    // 4. 使用 md-to-pdf 工具将临时 Markdown 文件转换为 PDF
    cp.execSync(`md-to-pdf ${tempMarkdownFile}`, { stdio: 'inherit' });

    // md-to-pdf 默认会生成与输入文件同名但后缀为 .pdf 的文件
    const generatedTempPDF = tempMarkdownFile.replace('.md', '.pdf');

    // 重命名为最终的输出文件名
    if (fs.existsSync(generatedTempPDF)) {
        fs.renameSync(generatedTempPDF, outputFilePDF);
        console.log(`[SUCCESS] PDF 生成成功: ${outputFilePDF}`);
    } else {
        console.warn(`[WARNING] 未找到预期的临时 PDF 文件: ${generatedTempPDF}`);
    }
} catch (error) {
     console.error(`[ERROR] 生成 PDF 时发生错误: ${error.message}`);
} finally {
    // 5. 自动清理临时的 Markdown 文件
    if (fs.existsSync(tempMarkdownFile)) {
        fs.unlinkSync(tempMarkdownFile);
    }
}

console.log('[INFO] 文档转换流程全部完成！');
