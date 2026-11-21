# -*- coding: utf-8 -*-
"""
水墨文字水印自动化处理与导入工具 (自适应墨迹视觉中心 + 统一对称留白)
"""

import os
import glob
import numpy as np
from PIL import Image

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
INPUT_DIR = os.path.join(BASE_DIR, 'tools', 'raw_watermarks')
OUTPUT_DIR = os.path.join(BASE_DIR, 'app', 'src', 'main', 'res', 'drawable')

GRID_MAP = [
    ("ink_cat_work", "工 / 工作"),
    ("ink_cat_learning", "学 / 学习"),
    ("ink_cat_daily", "常 / 日常"),
    ("ink_cat_emotion", "心 / 情绪"),
    ("ink_cat_method", "法 / 方法"),
    ("ink_cat_strategy", "策 / 策略"),
    ("ink_cat_social", "交 / 交际"),
    ("ink_cat_xiushen", "修 / 修身"),
    ("ink_cat_default", "省 / 三省")
]

def split_and_import_grid(grid_path, crop_size=420):
    """
    修正原大图 3x3 均分留白不一致问题：
    1. 寻找每个单元格内墨迹图案的实际视觉中心 (cx, cy)；
    2. 以真实中心为基准，以统一的正方形画幅 (crop_size x crop_size) 进行对称裁剪；
    3. 完整保留四周水墨装饰，同时保证每个字的上下左右留白均匀对称。
    """
    print(f"\n[自适应视觉中心与对称留白修正]：{grid_path}")
    img = Image.open(grid_path).convert('RGBA')
    W, H = img.size
    cw, ch = W // 3, H // 3
    
    pad = 120
    padded = Image.new('RGBA', (W + 2 * pad, H + 2 * pad), (0, 0, 0, 0))
    padded.paste(img, (pad, pad))
    
    arr = np.array(img)
    alpha = arr[:, :, 3] if 'A' in img.mode else (255 - np.mean(arr, axis=2))
    half = crop_size // 2
    
    count = 0
    for row in range(3):
        for col in range(3):
            res_id, label = GRID_MAP[count]
            y0, y1 = row * ch, (row + 1) * ch
            x0, x1 = col * cw, (col + 1) * cw
            patch_alpha = alpha[y0:y1, x0:x1]
            
            ys, xs = np.where(patch_alpha > 8)
            if len(ys) > 0:
                box_x0 = x0 + np.min(xs)
                box_x1 = x0 + np.max(xs)
                box_y0 = y0 + np.min(ys)
                box_y1 = y0 + np.max(ys)
                cx = (box_x0 + box_x1) / 2.0
                cy = (box_y0 + box_y1) / 2.0
            else:
                cx = (x0 + x1) / 2.0
                cy = (y0 + y1) / 2.0
                
            px = int(cx + pad)
            py = int(cy + pad)
            cell_slice = padded.crop((px - half, py - half, px + half, py + half))
            
            dest_file = os.path.join(OUTPUT_DIR, f"{res_id}.png")
            cell_slice.save(dest_file, 'PNG')
            print(f"  ✓ 第 {count+1}/9 格 [{label}] -> {res_id}.png (中心: {cx:.1f}, {cy:.1f}, 尺寸: {cell_slice.size})")
            count += 1
            
    print("\n[成功] 9 个分类水印已全量完成视觉居中与对称留白修正！")

if __name__ == '__main__':
    grid_img = os.path.join(INPUT_DIR, 'colored_transparent_grid.png')
    if os.path.exists(grid_img):
        split_and_import_grid(grid_img)
