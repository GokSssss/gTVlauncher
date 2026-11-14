"""Publish exact approved files. No builds, signing keys or personal tokens."""
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import tempfile
import zipfile

REPOSITORY = 'GokSssss/gTVlauncher'

def command(*args):
    return subprocess.check_output(list(args), text=True, timeout=180)

def api(path):
    return json.loads(command('gh', 'api', path))

def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def payload(root):
    data = json.loads((root / 'release.json').read_text())
    tag = data['tag']
    if data['repository'] != REPOSITORY or not re.fullmatch(r'v\d+\.\d+\.\d+', tag):
        raise ValueError('Unexpected repository or tag')
    if not re.fullmatch(r'[0-9a-f]{40}', data['source_commit']):
        raise ValueError('Source commit must be immutable')
    if not isinstance(data['release_id'], int) or data['release_id'] <= 0:
        raise ValueError('Existing draft release ID required')
    names = [item['name'] for item in data['files']]
    version = tag[1:]
    required = {version + '-True-TV-Launcher.apk', version + '-True-TV-Launcher-SOURCE.zip',
                version + '-True-TV-Launcher-Паспорт.txt', data['phone_version'] + '-To-TV.apk'}
    if len(names) != 4 or set(names) != required:
        raise ValueError('Exactly the four approved release files are required')
    for item in data['files']:
        if Path(item['name']).name != item['name'] or not re.fullmatch(r'[0-9a-f]{64}', item['sha256']):
            raise ValueError('Invalid artifact name or SHA256')
        path = root / item['name']
        if path.is_symlink() or path.stat().st_size != item['size'] or digest(path) != item['sha256']:
            raise ValueError('Payload mismatch: ' + item['name'])
        if path.suffix in ('.apk', '.zip'):
            with zipfile.ZipFile(path) as archive:
                if archive.testzip() is not None:
                    raise ValueError('ZIP CRC failed: ' + item['name'])
        print('Verified:', item['name'], item['size'], item['sha256'], flush=True)
    if not (root / 'release-notes.md').is_file():
        raise ValueError('Release notes missing')
    return data

def check_tag(data):
    ref = api('repos/' + REPOSITORY + '/git/ref/tags/' + data['tag'])
    if ref['object']['type'] != 'commit' or ref['object']['sha'] != data['source_commit']:
        raise ValueError('The version tag does not match the approved source commit')

def release(data):
    result = api('repos/' + REPOSITORY + '/releases/' + str(data['release_id']))
    if result['tag_name'] != data['tag']:
        raise ValueError('Unexpected release tag')
    return result

def check_download(data, item):
    with tempfile.TemporaryDirectory() as folder:
        command('gh', 'release', 'download', data['tag'], '--repo', REPOSITORY,
                '--pattern', item['name'], '--dir', folder)
        downloaded = Path(folder) / item['name']
        if downloaded.stat().st_size != item['size'] or digest(downloaded) != item['sha256']:
            raise ValueError('GitHub download mismatch: ' + item['name'])

def publish(root, data):
    if os.environ.get('GITHUB_REPOSITORY') != REPOSITORY or os.environ.get('RELEASE_TAG') != data['tag']:
        raise ValueError('This publisher runs only in the requested repository workflow')
    if not re.fullmatch(r'[0-9a-f]{40}', os.environ.get('PAYLOAD_COMMIT', '')):
        raise ValueError('An exact payload commit is required')
    check_tag(data)
    state = release(data)
    expected = {item['name'] for item in data['files']}
    existing = [asset['name'] for asset in state['assets']]
    if len(set(existing)) != len(existing) or set(existing) - expected:
        raise ValueError('Unexpected or duplicate release assets; nothing was deleted')
    for item in data['files']:
        if item['name'] in existing:
            check_download(data, item)
        elif state['draft']:
            command('gh', 'release', 'upload', data['tag'], str(root / item['name']), '--repo', REPOSITORY)
            check_download(data, item)
        else:
            raise ValueError('A published release is incomplete; refusing to change it')
    check_tag(data)
    if state['draft']:
        command('gh', 'release', 'edit', data['tag'], '--repo', REPOSITORY,
                '--title', data['title'], '--notes-file', str(root / 'release-notes.md'),
                '--draft=false', '--prerelease')
    final = release(data)
    if final['draft'] or not final['prerelease'] or {a['name'] for a in final['assets']} != expected:
        raise ValueError('Publication readback failed')
    for item in data['files']:
        check_download(data, item)
    print('Published and downloaded SHA256 verified:', final['html_url'], flush=True)
    summary = os.environ.get('GITHUB_STEP_SUMMARY')
    if summary:
        with open(summary, 'a') as output:
            output.write('Verified pre-release: ' + final['html_url'] + '\n\n')
            for item in data['files']:
                output.write('- `' + item['name'] + '` SHA256 `' + item['sha256'] + '`\n')

if __name__ == '__main__':
    root = Path(sys.argv[1]).resolve()
    data = payload(root)
    if '--verify-only' not in sys.argv[2:]:
        publish(root, data)
