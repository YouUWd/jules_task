#!/usr/bin/env node

/**
 * Mermaid 图像提取与渲染工具
 *
 * 该脚本用于将 Markdown 文件中包含的 Mermaid 代码块：
 * 单独渲染为高清晰度的 PNG 图片，并将替换了图片链接的 Markdown 内容输出。
 *
 * 依赖安装说明:
 * 运行此脚本前，请确保您已全局安装了 Node.js 及以下 npm 包：
 * npm install -g @mermaid-js/mermaid-cli
 *
 * 用法:
 * node generate-image.js <输入文件.md> [图像缩放比例]
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
    console.error('用法: node generate-image.js <输入文件.md> [图像缩放比例]');
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

console.error(`[INFO] 开始读取文档: ${inputFile}`);
console.error(`[INFO] 图像质量缩放比例设置为: ${scaleFactor}`);

if (!fs.existsSync(inputFile)) {
    console.error(`[ERROR] 找不到输入文件: ${inputFile}`);
    process.exit(1);
}

const content = fs.readFileSync(inputFile, 'utf-8');
const mermaidRegex = /```mermaid([\s\S]*?)```/g;
let match;
let count = 1;
let newContent = content;

console.error('[INFO] 开始提取并渲染 Mermaid 图表...');

// 遍历所有的 mermaid 代码块
while ((match = mermaidRegex.exec(content)) !== null) {
    const mmContent = match[1].trim();
    const tempFile = `temp_mermaid_${count}.mmd`;
    const finalImage = `${baseName}-Mermaid-${count}.png`;

    // 1. 将 mermaid 代码块保存到临时 .mmd 文件
    fs.writeFileSync(tempFile, mmContent);
    console.error(`[INFO] 正在生成图片: ${finalImage}`);

    try {
        // 使用 mmdc 渲染为 PNG 图片。通过 -s 参数控制 device scale factor (图像质量)
        // 使用 ignore 阻止 mmdc 输出污染 stdout
        cp.execSync(`mmdc -i ${tempFile} -o ${finalImage} -s ${scaleFactor} -b white`, { stdio: 'ignore' });

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

console.error('[INFO] Mermaid 渲染完成。');

// 将替换过图片链接的 Markdown 内容输出到标准输出，以便管道调用或重定向
console.log(newContent);
