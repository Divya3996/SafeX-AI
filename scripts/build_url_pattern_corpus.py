#!/usr/bin/env python3
"""Safe authored regression examples; no URL is resolved or opened."""
import base64
import json
from pathlib import Path
from urllib.parse import quote

ROOT = Path(__file__).resolve().parents[1]
cases = []
def add(family, url, expected='WARN', note='Structural warning; not verified live fraud.'):
    cases.append(dict(id=f'{family}-{sum(c["family"] == family for c in cases)+1:02}', family=family,
                      url=url, expected=expected, note=note))

for host in ['paypa1.example','paypl.example','payapl.example','paypall.example','p4ypal.example',
             'micros0ft.example','microsof.example','microsoftt.example','amaz0n.example','netf1ix.example']:
    add('brand-typo', f'https://{host}/login')
for host in ['paypal-verify.example','secure-paypal.example','paypalaccount.example','loginpaypal.example',
             'paypal-support-service.example','hdfc-kyc.example','sbi-update.example','phonepe-refund.example']:
    add('brand-affix', f'https://{host}/')
for host in ['paypal.com.attacker.example','www.paypal.com.attacker.example',
             'accounts.google.com.attacker.example','login.microsoftonline.com.attacker.example']:
    add('trusted-domain-prefix', f'https://{host}/login')
for host in ['paypal.login.attacker.example','microsoft.verify.attacker.example','sbi.kyc.attacker.example']:
    add('branded-subdomain', f'https://{host}/')
for host in ['раураl.example','аррӏе.example','gοοgle.example','microsоft.example']:
    add('unicode-homograph', f'https://{host}/login')
    add('unicode-homograph', f'https://{host.encode("idna").decode()}/login')
for value in ['https://paypal.com@attacker.example/login','https://user:pass@attacker.example/',
              'https://google.com%40support@attacker.example/']:
    add('userinfo',value)
for value in ['https://203.0.113.52/login','https://[2001:db8::52]/login',
              'http://2130706433/login','https://0x7f000001/login','https://0177.0.0.1/login','https://127.1/login']:
    add('numeric-host',value)
for destination in ['https://paypal-verify.example/login','//paypal-verify.example/login']:
    add('redirect', 'https://portal.example/?next='+destination)
    for layers in range(1,5):
        encoded=destination
        for _ in range(layers):encoded=quote(encoded,safe='')
        add('layered-redirect','https://portal.example/?redirect='+encoded)
    add('base64-redirect','https://portal.example/?url='+base64.urlsafe_b64encode(destination.encode()).decode())
add('nested-redirect','https://portal.example/?next='+quote('https://middle.example/?url=https://paypal-verify.example',safe=''))
for host in ['bit.ly','tinyurl.com','t.co','cutt.ly','is.gd','ow.ly','lnkd.in','rebrand.ly']:
    add('shortened-destination',f'https://{host}/ExampleDemo',note='Hidden destination caution; shorteners are not intrinsically phishing.')
for url in ['https://account-verify.attacker.example/','https://bank-kyc-update.attacker.example/',
            'https://free-gift-claim.attacker.example/','https://wallet-connect.attacker.example/',
            'https://otp-verification.attacker.example/']:
    add('sensitive-host-cues',url)
for host in ['paypal-verify.pages.dev','microsoft-login.netlify.app','sbi-kyc.vercel.app','paypal-login.github.io']:
    add('hosted-tenant',f'https://{host}/')
for path in ['invoice.pdf.exe','bank-statement.pdf.apk','photo.jpg.scr','refund.docm.exe',
             'invoice%2Epdf%2Eapk','statement.pdf%20.exe','security-update.apk','script.ps1']:
    add('executable-download',f'https://files.example/{path}')
for url in ['https://attacker.example/paypal.com/login','https://attacker.example/login#https://paypal.com',
            'https://attacker.example/?login=https://paypal.com']:
    add('brand-outside-host',url)
for url in ['http://account-verify.example:8080/login','https://bank--verify--reward.example/login',
            'https://a.b.c.d.attacker.example/verify']:
    add('combined-structure',url)
for url in ['https://paypa1.example./login','HTTPS://PAYPA1.EXAMPLE/Login']:
    add('canonical-evasion',url)
for url in ['https://example.com\\@attacker.example/','https://exa\u200bmple.com/',
            'https://example.com/\u202eexe.pdf','https://%70aypal.com.attacker.example/',
            'https://example.com:99999/','https://user@@example.com/','https://example.com/?x=%ZZ',
            'javascript:alert(1)','data:text/html,example','intent://example/#Intent;scheme=https;end','file:///tmp/example']:
    add('rejected-ambiguity',url,'REJECT', 'Unsupported or ambiguous input must not be opened.')
for url in ['https://example.com/','https://www.paypal.com/signin','https://paypal.com./signin',
            'https://accounts.google.com/login','https://login.microsoftonline.com/common/oauth2/authorize',
            'https://support.apple.com/','https://www.amazon.in/','https://onlinesbi.sbi/',
            'https://hdfcbank.com/','https://icicibank.com/','https://phonepe.com/',
            'https://docs.example/guides/phishing/paypal.com','https://example.com/?email=person@example.com',
            'https://example.com/path/a+b','https://example.com/?next=%2Fdashboard',
            'https://example.com/?next=https://example.com/dashboard',
            'https://example.com/?redirect=https%3A%2F%2Fexample.com%2Fdashboard',
            'https://example.com/?url=https://example.com/help',
            'https://example.com/search?q=https://example.com/help',
            'https://github.io/','https://ordinary-project.pages.dev/',
            'https://xn--bcher-kva.example/','https://bücher.example/',
            'https://हिन्दी.example/','https://ગુજરાતી.example/',
            'https://apply.example/','https://ubi.example/',
            'https://example.com/%E0%A4%B9%E0%A4%BF%E0%A4%A8%E0%A5%8D%E0%A4%A6%E0%A5%80',
            'https://example.com/?utm_source=mail&utm_campaign=launch&gclid=abc',
            'https://download.example/manual.pdf','https://example.com/static/app.js?build=42']:
    add('benign-control',url,'ALLOW','Authored benign control, not a live safety certification.')
for url, note in [
    ('https://ordinary.example/','Compromised legitimate website: URL text alone cannot expose page content.'),
    ('https://ordinary.example/a7Qf','Opaque redirect or newly registered clean-looking domain.'),
    ('https://docs.google.com/forms/d/example/viewform','Legitimate hosted form can be abused; content unavailable offline.'),
    ('https://login.microsoftonline.com/common/oauth2/deviceauth','Device-code or consent lure needs communication context.'),
]:add('offline-blind-spot',url,'UNKNOWN',note)

document={'schema_version':1,'kind':'authored regression corpus, not independent accuracy data','cases':cases}
paths=[ROOT/'models/url-pattern-corpus.json',
       ROOT/'android-app/app/src/test/resources/url-pattern-corpus.json',
       ROOT/'android-app/app/src/androidTest/assets/url-pattern-corpus.json']
for path in paths:path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(document,ensure_ascii=False,indent=2)+'\n')
print(f'{len(cases)} safe authored examples in {len(set(c["family"] for c in cases))} families.')
