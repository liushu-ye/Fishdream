"""由一张方形源图生成 Android 图标资源（自适应图标 + 各密度传统图标）。

用法：
    python tools/make_icons.py [源图路径]

默认源图：D:\\安装包\\临时笔记取名讨论 (1).png

策略：
- 源图是整幅不透明的近白底方形图，内容集中在中间。
- 自适应图标：背景用源图底色，前景把整幅图缩放，使「内容外接圆 = SAFE_DP」，
  保证圆/圆角方/超椭圆等任何遮罩都不会裁到内容，且因底色一致而无接缝。
- 传统图标：整幅图作为方形图标；圆形版做圆形裁切。
"""
import os
import sys
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.normpath(os.path.join(HERE, '..', 'app', 'src', 'main', 'res'))
DEFAULT_SRC = r'D:\安装包\临时笔记取名讨论 (1).png'

BG = (254, 254, 254)   # 源图底色，用作自适应图标背景
ARTWORK_DP = 108       # 自适应图标画布
LEGACY_DP = 48         # 传统图标画布
SAFE_DP = 68           # 保证可见区域是 72dp，这里再留约 5% 余量，避免抗锯齿边缘被裁

DENSITIES = {'mdpi': 1.0, 'hdpi': 1.5, 'xhdpi': 2.0, 'xxhdpi': 3.0, 'xxxhdpi': 4.0}


def content_bbox(im, bg, probe=512, threshold=60):
    """返回远离底色的内容外接框（原图坐标系）。"""
    small = im.resize((probe, probe), Image.LANCZOS)
    mask = Image.new('L', (probe, probe))
    mp, pp = mask.load(), small.load()
    for y in range(probe):
        for x in range(probe):
            r, g, b = pp[x, y]
            mp[x, y] = 255 if (abs(r - bg[0]) + abs(g - bg[1]) + abs(b - bg[2])) > threshold else 0
    bb = mask.getbbox()
    if not bb:
        raise SystemExit('源图里没有找到任何内容')
    k = im.size[0] / float(probe)
    return tuple(int(v * k) for v in bb)


def emit(path, img):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path, 'PNG', optimize=True)
    print('  wrote %-68s %s' % (os.path.relpath(path, RES), img.size))


def main():
    src = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_SRC
    im = Image.open(src).convert('RGB')
    W, H = im.size
    print('source : %s  %dx%d' % (src, W, H))

    l, t, r, b = content_bbox(im, BG)
    cw, ch = r - l, b - t
    diag = (cw ** 2 + ch ** 2) ** 0.5
    print('content: bbox=(%d,%d,%d,%d)  %dx%d  diagonal=%.1f  (%.1f%% x %.1f%% of canvas)'
          % (l, t, r, b, cw, ch, diag, 100.0 * cw / W, 100.0 * ch / H))

    for name, factor in DENSITIES.items():
        folder = os.path.join(RES, 'mipmap-' + name)

        canvas_px = int(round(ARTWORK_DP * factor))
        safe_px = SAFE_DP * factor
        art_side = int(round(safe_px * W / diag))          # 使内容外接圆 = 安全区
        art = im.resize((art_side, art_side), Image.LANCZOS)
        fg = Image.new('RGBA', (canvas_px, canvas_px), (0, 0, 0, 0))
        off = (canvas_px - art_side) // 2
        fg.paste(art, (off, off))
        emit(os.path.join(folder, 'ic_launcher_foreground.png'), fg)

        legacy_px = int(round(LEGACY_DP * factor))
        emit(os.path.join(folder, 'ic_launcher.png'), im.resize((legacy_px, legacy_px), Image.LANCZOS))

        circle = Image.new('RGBA', (legacy_px, legacy_px), (0, 0, 0, 0))
        circle.paste(im.resize((legacy_px, legacy_px), Image.LANCZOS), (0, 0))
        m = Image.new('L', (legacy_px * 4, legacy_px * 4), 0)
        ImageDraw.Draw(m).ellipse((0, 0, legacy_px * 4 - 1, legacy_px * 4 - 1), fill=255)
        circle.putalpha(m.resize((legacy_px, legacy_px), Image.LANCZOS))
        emit(os.path.join(folder, 'ic_launcher_round.png'), circle)

    print('done.')


if __name__ == '__main__':
    main()
