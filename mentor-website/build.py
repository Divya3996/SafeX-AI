"""Render the complete, controlled meeting-plan Markdown without dependencies."""
from pathlib import Path
import html
import re
from urllib.parse import quote

ROOT = Path(__file__).resolve().parent
DOCUMENT = (ROOT / 'source/meeting-plan.md').read_text()
SECTIONS = [
    ('overview', 'The pitch', 'Start here'),
    ('problem', 'Problem & people', None),
    ('goals', 'Product goals', None),
    ('features', 'Working features', 'The product'),
    ('experience', 'User experience', None),
    ('architecture', 'Technical design', None),
    ('privacy', 'Privacy & permissions', None),
    ('validation', 'Evidence & limits', 'Proof & next steps'),
    ('roadmap', 'Development roadmap', None),
    ('evaluation', 'Evaluation & targets', None),
    ('demo', 'Six-minute demo', 'The meeting'),
    ('mentor-questions', 'Mentor decisions', None),
    ('presentation', 'Presentation outline', None),
    ('resources', 'Supporting material', None),
]
LINKS = {
    'validation.md': 'resources/validation.md',
    'test-results/summary.json': 'resources/test-summary.json',
    'ml-model.md': 'resources/ml-model.md',
    'architecture.md': 'resources/architecture.md',
    'features.md': 'resources/features.md',
    'demo.md': 'resources/demo.md',
    '../demo-assets/README.md': 'resources/synthetic-demo-assets.md',
    'languages-and-branding.md': 'resources/languages-and-branding.md',
    '../branding/safex-logo.svg': 'assets/safex-logo.svg',
}

def inline(text):
    tokens = []
    def keep(value):
        tokens.append(value)
        return f'\x00{len(tokens)-1}\x00'
    def link(match):
        label, target = match.groups()
        if target.endswith('.apk'):
            return keep(f'<span class="project-artifact">{html.escape(label)} <small>Project artifact · request for rehearsal</small></span>')
        target = LINKS.get(target, target)
        download = ' download' if target.startswith('resources/') else ''
        return keep(f'<a href="{html.escape(target, quote=True)}"{download}>{html.escape(label)}</a>')
    text = re.sub(r'`([^`]+)`', lambda m: keep(f'<code>{html.escape(m[1])}</code>'), text)
    text = re.sub(r'\[([^\]]+)\]\(([^)]+)\)', link, text)
    text = html.escape(text)
    text = re.sub(r'\*\*(.+?)\*\*', r'<strong>\1</strong>', text)
    return re.sub(r'\x00(\d+)\x00', lambda m: tokens[int(m[1])], text)

def render(body, section_label):
    lines = body.strip().splitlines()
    blocks = []
    i = 0
    while i < len(lines):
        line = lines[i].strip()
        if not line:
            i += 1
        elif line.startswith('```'):
            i += 1
            code = []
            while i < len(lines) and not lines[i].strip().startswith('```'):
                code.append(lines[i])
                i += 1
            blocks.append('<div class="pipeline"><div class="code-label">One shared, local pipeline</div><pre><code>' + html.escape('\n'.join(code)) + '</code></pre></div>')
            i += 1
        elif line.startswith('### '):
            blocks.append('<h3>' + inline(line[4:]) + '</h3>')
            i += 1
        elif line.startswith('|'):
            rows = []
            while i < len(lines) and lines[i].strip().startswith('|'):
                cells = [x.strip() for x in lines[i].strip().strip('|').split('|')]
                if not all(re.fullmatch(r':?-+:?', cell) for cell in cells):
                    rows.append(cells)
                i += 1
            head = '<thead><tr>' + ''.join('<th scope="col">' + inline(c) + '</th>' for c in rows[0]) + '</tr></thead>'
            rest = '<tbody>' + ''.join('<tr>' + ''.join('<td>' + inline(c) + '</td>' for c in row) + '</tr>' for row in rows[1:]) + '</tbody>'
            blocks.append(f'<div class="table-wrap" tabindex="0" role="region" aria-label="{html.escape(section_label)} table">' + '<table>' + head + rest + '</table></div>')
        elif line.startswith('>'):
            quote_lines = []
            while i < len(lines) and lines[i].strip().startswith('>'):
                quote_lines.append(lines[i].strip()[1:].strip())
                i += 1
            blocks.append('<blockquote><p>' + inline(' '.join(quote_lines)) + '</p></blockquote>')
        elif re.match(r'(- |\d+\. )', line):
            ordered = bool(re.match(r'\d+\. ', line))
            pattern = r'\d+\. (.*)' if ordered else r'- (.*)'
            items = []
            while i < len(lines) and (m := re.match(pattern, lines[i].strip())):
                items.append('<li>' + inline(m[1]) + '</li>')
                i += 1
            tag = 'ol' if ordered else 'ul'
            blocks.append(f'<{tag}>' + ''.join(items) + f'</{tag}>')
        else:
            parts = [line]
            i += 1
            while i < len(lines) and lines[i].strip() and not re.match(r'(### |\||>|```|- |\d+\. )', lines[i].strip()):
                parts.append(lines[i].strip())
                i += 1
            blocks.append('<p>' + inline(' '.join(parts)) + '</p>')
    return '\n'.join(blocks)

matches = list(re.finditer(r'^## (\d+)\. (.+)$', DOCUMENT, re.M))
assert len(matches) == len(SECTIONS), 'Every meeting-plan section must appear on the website.'
nav = []
sections = []
for i, (match, (section_id, label, group)) in enumerate(zip(matches, SECTIONS)):
    if group:
        nav.append(f'<p class="nav-group">{group}</p>')
    nav.append(f'<a href="#{section_id}" data-section="{section_id}"'+(' aria-current="location"' if i == 0 else '')+f'><span class="nav-number">{i+1:02}</span><span>{label}</span></a>')
    end = matches[i+1].start() if i+1 < len(matches) else len(DOCUMENT)
    body = DOCUMENT[match.end():end]
    content = render(body, label)
    if section_id == 'features':
        content += '''<div class="app-evidence"><div class="evidence-heading"><span class="eyebrow">From the working build</span><h3>One app. Three languages.</h3><p>Actual SafeX AI screens. The Gujarati example uses 150% text size; both warning examples are synthetic.</p></div><div class="screenshot-row"><figure><a href="assets/dashboard.png" target="_blank" rel="noopener"><img src="assets/dashboard.png" width="720" height="1600" loading="lazy" alt="SafeX AI dashboard showing local protection status and scan controls"></a><figcaption>Protection dashboard</figcaption></figure><figure><a href="assets/scam-result.png" target="_blank" rel="noopener"><img src="assets/scam-result.png" width="720" height="1600" loading="lazy" alt="English SafeX AI warning with risk evidence for a synthetic scam"></a><figcaption>Evidence in English</figcaption></figure><figure><a href="assets/scam-result-gujarati-150.png" target="_blank" rel="noopener"><img src="assets/scam-result-gujarati-150.png" width="720" height="1600" loading="lazy" alt="Gujarati SafeX AI warning with large text for a synthetic scam"></a><figcaption><span lang="gu">ગુજરાતી</span> · 150% text</figcaption></figure></div></div>'''
    sections.append(f'<section id="{section_id}" class="document-section" aria-labelledby="heading-{section_id}"><div class="section-heading"><span class="section-number">{i+1:02}</span><div><span class="eyebrow">{label}</span><h2 id="heading-{section_id}">{html.escape(match[2])}</h2></div></div><div class="section-content">{content}</div></section>')

favicon = 'data:image/svg+xml,' + quote((ROOT/'dist/assets/safex-logo.svg').read_text(), safe='')
template = (ROOT/'template.html').read_text()
page = template.replace('{{NAV}}', '\n'.join(nav)).replace('{{SECTIONS}}', '\n'.join(sections)).replace('{{FAVICON}}', favicon)
(ROOT/'dist/index.html').write_text(page)
print(f'Rendered all {len(sections)} sections from the complete meeting document.')
