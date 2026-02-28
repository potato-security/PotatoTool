#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
POC 清单生成脚本

读取 POC 目录结构，生成云端 poc.json 清单文件。

使用方法:
    python generate_poc_manifest.py <poc_directory> [output_file]
示例:
    python generate_poc_manifest.py ./src/main/java/com/potato/potatotool/content/redTeam/vulnScanner/poc/
    python generate_poc_manifest.py ./poc_cloud/ poc.json
"""

import os
import sys
import json
import hashlib
from datetime import datetime
from pathlib import Path


def calculate_checksum(file_path):
    """计算文件的 MD5 校验和"""
    md5_hash = hashlib.md5()
    with open(file_path, "rb") as f:
        for byte_block in iter(lambda: f.read(4096), b""):
            md5_hash.update(byte_block)
    return md5_hash.hexdigest()


def get_file_size(file_path):
    """获取文件大小（字节）"""
    return os.path.getsize(file_path)


def is_poc_file(filename):
    """判断是否是 POC 文件"""
    lower_name = filename.lower()
    return lower_name.endswith('.yaml') or lower_name.endswith('.yml') or lower_name.endswith('.json')


def normalize_dir_path(dir_path):
    if not dir_path or dir_path == '.':
        return '.'
    return str(dir_path).replace('\\', '/').strip('/')


def make_file_key(md5):
    """使用MD5作为文件唯一标识（与Java保持一致）"""
    return md5


def make_deleted_id(dir_path, filename):
    d = normalize_dir_path(dir_path)
    if d == '.':
        return filename
    return f"{d}/{filename}"


def bump_version(version_str):
    """版本号 +0.0.1，按每段逢 10 进位（例如 1.0.9 -> 1.1.0）。"""
    parts = [p.strip() for p in str(version_str).split('.') if p.strip() != '']
    nums = []
    for p in parts:
        try:
            nums.append(int(p))
        except ValueError:
            nums.append(0)

    while len(nums) < 3:
        nums.append(0)
    if len(nums) > 3:
        nums = nums[:3]

    carry = 1
    for i in range(len(nums) - 1, 0, -1):
        if carry == 0:
            break
        nums[i] += carry
        if nums[i] >= 10:
            nums[i] -= 10
            carry = 1
        else:
            carry = 0

    if carry:
        nums[0] += carry

    return '.'.join(str(n) for n in nums)


def strip_md5_prefix(value):
    """移除 md5: 前缀"""
    if not isinstance(value, str):
        return None
    v = value.strip()
    if v.startswith('md5:'):
        return v.split(':', 1)[1]
    return v


def load_existing_manifest(manifest_path):
    if not manifest_path or not os.path.isfile(manifest_path):
        return None
    try:
        with open(manifest_path, 'r', encoding='utf-8') as f:
            return json.load(f)
    except Exception:
        return None


def build_old_index(old_manifest):
    """构建旧清单索引，使用MD5作为key"""
    old_index = {}  # key: md5, value: {version, md5, size, path, name}
    old_id_map = {}  # key: md5, value: deleted_id

    if not isinstance(old_manifest, dict):
        return old_index, old_id_map, [], None

    for dir_entry in old_manifest.get('directories', []) or []:
        dir_path = dir_entry.get('path', '.')
        for file_entry in dir_entry.get('files', []) or []:
            name = file_entry.get('name')
            if not name:
                continue

            old_md5 = file_entry.get('md5')
            if old_md5 is None:
                old_md5 = strip_md5_prefix(file_entry.get('checksum'))
            else:
                old_md5 = strip_md5_prefix(old_md5)
            
            if not old_md5:
                continue
            
            # 使用MD5作为key
            key = make_file_key(old_md5)

            old_index[key] = {
                'version': file_entry.get('version', '1.0.0'),
                'md5': old_md5,
                'size': file_entry.get('size'),
                'path': dir_path,
                'name': name,
            }
            old_id_map[key] = make_deleted_id(dir_path, name)

    old_deleted = old_manifest.get('deleted', [])
    if not isinstance(old_deleted, list):
        old_deleted = []

    normalized_deleted = []
    for item in old_deleted:
        if isinstance(item, str):
            normalized_deleted.append({'id': item})
        elif isinstance(item, dict):
            md5 = item.get('md5')
            if md5 is None:
                md5 = strip_md5_prefix(item.get('checksum'))
            else:
                md5 = strip_md5_prefix(md5)
            obj = dict(item)
            if md5 is not None:
                obj['md5'] = md5
                if 'checksum' in obj:
                    obj.pop('checksum', None)
            normalized_deleted.append(obj)

    old_manifest_version = old_manifest.get('version')

    return old_index, old_id_map, normalized_deleted, old_manifest_version


def scan_poc_directory(root_dir):
    """
    扫描 POC 目录，生成目录结构
    
    Returns:
        dict: 目录结构，格式为 {相对目录路径: [文件列表]}
    """
    directories = {}
    root_path = Path(root_dir).resolve()

    for dirpath, dirnames, filenames in os.walk(root_dir):
        # 过滤隐藏文件和目录
        dirnames[:] = [d for d in dirnames if not d.startswith('.')]

        # 获取相对路径
        rel_dir = Path(dirpath).resolve().relative_to(root_path)
        rel_dir_str = str(rel_dir).replace('\\', '/')

        # 如果是根目录，使用空字符串
        if rel_dir_str == '.':
            rel_dir_str = ''

        # 收集 POC 文件
        poc_files = []
        for filename in filenames:
            if is_poc_file(filename) and not filename.startswith('.'):
                file_path = os.path.join(dirpath, filename)
                poc_files.append({
                    'name': filename,
                    'path': file_path,
                    'size': get_file_size(file_path),
                    "md5": calculate_checksum(file_path)
                })

        if poc_files:
            directories[rel_dir_str] = poc_files

    return directories


def generate_manifest(poc_dir, old_index=None, old_id_map=None, old_deleted=None, old_manifest_version=None):
    """
    生成 POC 清单
    
    Args:
        poc_dir: POC 目录路径
        version: 版本号，默认使用日期
        
    Returns:
        dict: 清单数据
    """
    if old_index is None:
        old_index = {}
    if old_id_map is None:
        old_id_map = {}
    if old_deleted is None:
        old_deleted = []

    directories = scan_poc_directory(poc_dir)

    manifest = {
        "version": "1.0.0",
        "lastUpdate": datetime.utcnow().isoformat() + "Z",
        "directories": [],
        "deleted": []
    }

    total_files = 0
    current_md5_set = set()  # 用于去重和检测删除
    skipped_duplicates = []  # 记录跳过的重复文件

    added_count = 0
    updated_count = 0
    kept_count = 0
    skipped_count = 0

    added_files = []
    updated_files = []

    for dir_path, files in sorted(directories.items()):
        manifest_dir_path = dir_path if dir_path else "."
        dir_entry = {
            "path": manifest_dir_path,
            "files": []
        }

        for file_info in sorted(files, key=lambda x: x['name']):
            file_md5 = file_info['md5']
            file_id = make_deleted_id(manifest_dir_path, file_info['name'])
            
            # 使用MD5作为唯一标识（与Java保持一致）
            key = make_file_key(file_md5)
            
            # 检查是否已存在相同MD5的文件（内容重复）
            if file_md5 in current_md5_set:
                skipped_count += 1
                skipped_duplicates.append({
                    'path': manifest_dir_path,
                    'name': file_info['name'],
                    'md5': file_md5,
                })
                continue
            
            current_md5_set.add(file_md5)

            old = old_index.get(key)
            if old:
                # MD5相同说明内容未变，保持原有版本号
                old_version = old.get('version', '1.0.0')
                file_version = old_version
                action = 'keep'
                kept_count += 1
            else:
                file_version = '1.0.0'
                action = 'add'
                added_count += 1
                added_files.append({
                    'id': file_id,
                    'new_version': file_version,
                })

            file_entry = {
                "name": file_info['name'],
                "version": file_version,
                "md5": file_md5,
                "action": action,
                "size": file_info['size']
            }
            dir_entry["files"].append(file_entry)
            total_files += 1

        if dir_entry["files"]:  # 只添加非空目录
            manifest["directories"].append(dir_entry)

    deleted_auto_entries = []
    deleted_files = []
    for old_key, old_meta in old_index.items():
        # old_key 现在是 MD5
        if old_key not in current_md5_set:
            del_id = old_id_map.get(old_key, old_key)
            old_path = old_meta.get('path', '.')
            name_part = old_meta.get('name', '')

            entry = {
                'id': del_id,
                'path': old_path,
                'name': name_part,
                'version': old_meta.get('version', '1.0.0'),
                'md5': old_meta.get('md5'),
                'size': old_meta.get('size'),
            }
            deleted_auto_entries.append(entry)
            deleted_files.append({
                'id': del_id,
                'old_version': old_meta.get('version', '1.0.0'),
                'md5': old_meta.get('md5'),
            })

    merged_deleted = []
    seen = set()

    def should_keep_deleted_item(item):
        if not isinstance(item, dict):
            return False
        item_id = item.get('id')
        if item_id and item_id in current_ids:
            return False
        md5 = item.get('md5')
        if md5 is not None:
            md5 = strip_md5_prefix(md5)
        if md5 and md5 in current_md5:
            return False
        return True

    for item in (old_deleted + deleted_auto_entries):
        if not isinstance(item, dict):
            continue
        normalized = dict(item)
        if 'checksum' in normalized and 'md5' not in normalized:
            normalized['md5'] = strip_md5_prefix(normalized.get('checksum'))
            normalized.pop('checksum', None)
        if not should_keep_deleted_item(normalized):
            continue

        dedup_key = normalized.get('id') or normalized.get('md5')
        if not dedup_key:
            dedup_key = json.dumps(normalized, sort_keys=True, ensure_ascii=False)
        if dedup_key in seen:
            continue
        seen.add(dedup_key)
        merged_deleted.append(normalized)

    manifest["deleted"] = merged_deleted

    has_changes = (added_count + updated_count + len(deleted_auto_entries)) > 0

    if old_manifest_version:
        manifest_version = bump_version(old_manifest_version) if has_changes else old_manifest_version
    else:
        manifest_version = "1.0.0"

    manifest["version"] = manifest_version

    stats = {
        'added': added_count,
        'updated': updated_count,
        'kept': kept_count,
        'skipped': skipped_count,  # 内容重复跳过
        'deleted_new': len(deleted_auto_entries),
        'deleted_total': len(merged_deleted),
    }

    changes = {
        'added': added_files,
        'updated': updated_files,
        'deleted': deleted_files,
        'skipped': skipped_duplicates,  # 跳过的重复文件列表
    }

    return manifest, total_files, stats, changes, old_manifest_version, manifest_version


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        sys.exit(1)

    poc_dir = sys.argv[1]
    output_file = sys.argv[2] if len(sys.argv) > 2 else "poc.json"

    if not os.path.isdir(poc_dir):
        print(f"错误: 目录不存在 - {poc_dir}")
        sys.exit(1)

    print(f"扫描 POC 目录: {poc_dir}")

    old_manifest = load_existing_manifest(output_file)
    old_index, old_id_map, old_deleted, old_manifest_version = build_old_index(old_manifest)

    manifest, total_files, stats, changes, old_ver, new_ver = generate_manifest(
        poc_dir,
        old_index=old_index,
        old_id_map=old_id_map,
        old_deleted=old_deleted,
        old_manifest_version=old_manifest_version,
    )

    # 写入 JSON 文件
    with open(output_file, 'w', encoding='utf-8') as f:
        json.dump(manifest, f, indent=2, ensure_ascii=False)

    print(f"已生成清单文件: {output_file}")
    if old_ver:
        if old_ver == new_ver:
            print(f"  - 清单版本: {new_ver} (无变化)")
        else:
            print(f"  - 清单版本: {old_ver} -> {new_ver}")
    else:
        print(f"  - 清单版本: {new_ver}")
    print(f"  - 目录数: {len(manifest['directories'])}")
    print(f"  - 文件数: {total_files}")
    print(f"  - 新增: {stats['added']}")
    print(f"  - 更新: {stats['updated']}")
    print(f"  - 未变: {stats['kept']}")
    print(f"  - 跳过(内容重复): {stats['skipped']}")
    print(f"  - 删除(本次识别): {stats['deleted_new']}")
    print(f"  - deleted总数: {stats['deleted_total']}")

    log_file = os.path.join(os.path.dirname(output_file) or '.', 'upDatePoc.log')
    if changes['added'] or changes['updated'] or changes['deleted']:
        with open(log_file, 'a', encoding='utf-8') as lf:
            if old_ver and old_ver == new_ver:
                lf.write(f"[{datetime.now().isoformat()}] manifest {new_ver} (no change)\n")
            elif old_ver:
                lf.write(f"[{datetime.now().isoformat()}] manifest {old_ver} -> {new_ver}\n")
            else:
                lf.write(f"[{datetime.now().isoformat()}] manifest {new_ver}\n")

            for item in changes['added']:
                lf.write(f"ADD    {item['id']} (version={item['new_version']})\n")
            for item in changes['updated']:
                lf.write(f"UPDATE {item['id']} ({item['old_version']} -> {item['new_version']})\n")
            for item in changes['deleted']:
                lf.write(f"DELETE {item['id']} (old version={item['old_version']}, md5={item.get('md5')})\n")
            lf.write('\n')
        print(f"  - 变更明细已写入日志: {log_file}")


if __name__ == "__main__":
    main()
