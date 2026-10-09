#!/usr/bin/env python3
"""Author controlled functional fixtures. These are development tests, never accuracy certification."""
from pathlib import Path
import json
ROOT = Path(__file__).resolve().parents[1]
scams = [
('credentials', 'Share your OTP and password immediately to avoid bank account suspension.', 'बैंक खाता बंद होने से बचाने के लिए तुरंत अपना ओटीपी और पासवर्ड भेजें।', 'બેંક ખાતું બંધ થવાથી બચાવવા માટે તરત તમારો ઓટીપી અને પાસવર્ડ મોકલો.'),
('reward', 'You won a lottery prize. Pay a fee to claim it.', 'आपने लॉटरी का इनाम जीता है। इसे पाने के लिए शुल्क जमा करें।', 'તમે લોટરીનું ઇનામ જીત્યા છો. તેને મેળવવા માટે ફી જમા કરો.'),
('job', 'Work from home job confirmed. Pay registration fee first.', 'घर से काम की नौकरी पक्की है। पहले पंजीकरण शुल्क जमा करें।', 'ઘરેથી કામની નોકરી નક્કી છે. પહેલા રજિસ્ટ્રેશન ફી જમા કરો.'),
('loan', 'Your loan is approved. Deposit the processing fee before disbursement.', 'आपका लोन मंजूर है। पैसे मिलने से पहले प्रोसेसिंग शुल्क जमा करें।', 'તમારી લોન મંજૂર છે. પૈસા મળતા પહેલાં પ્રોસેસિંગ ફી જમા કરો.'),
('upi', 'Scan this QR and enter your UPI PIN to receive a refund.', 'रिफंड पाने के लिए यह क्यूआर स्कैन करें और अपना यूपीआई पिन दर्ज करें।', 'રિફંડ મેળવવા માટે આ ક્યુઆર સ્કેન કરો અને તમારો યુપીઆઈ પિન દાખલ કરો.'),
('coercion', 'Police digital arrest warrant: transfer money and keep this secret.', 'पुलिस की डिजिटल गिरफ्तारी: तुरंत पैसे भेजें और किसी को मत बताओ।', 'પોલીસની ડિજિટલ અરેસ્ટ: તરત પૈસા મોકલો અને કોઈને કહેશો નહીં.'),
('remote_access', 'Bank support asks you to install AnyDesk and share the access code.', 'बैंक सहायता के लिए रिमोट एक्सेस दें और अपना पासवर्ड भेजें।', 'બેંક સહાય માટે એનીડેસ્ક ઇન્સ્ટોલ કરો અને તમારો પાસવર્ડ મોકલો.'),
('courier', 'Urgent customs clearance. Share your bank password and pay immediately.', 'तुरंत कस्टम मंजूरी के लिए अपना बैंक पासवर्ड भेजें और भुगतान करें।', 'તાત્કાલિક કસ્ટમ મંજૂરી માટે તમારો બેંક પાસવર્ડ મોકલો અને ચુકવણી કરો.'),
]
benign = [
('safety_advice', 'Never share your OTP or password.', 'अपना ओटीपी और पासवर्ड कभी साझा न करें।', 'તમારો ઓટીપી અને પાસવર્ડ ક્યારેય શેર ન કરો.'),
('safety_advice', 'Do not scan a QR code to receive a refund.', 'रिफंड पाने के लिए क्यूआर स्कैन मत करें।', 'રિફંડ મેળવવા માટે ક્યુઆર સ્કેન ન કરો.'),
('everyday', 'Your meeting is at 3 PM tomorrow.', 'आपकी बैठक कल दोपहर तीन बजे है।', 'તમારી બેઠક કાલે બપોરે ત્રણ વાગ્યે છે.'),
('receipt', 'Payment receipt received. No action is required.', 'भुगतान की रसीद मिल गई। किसी कार्रवाई की आवश्यकता नहीं है।', 'ચુકવણીની રસીદ મળી ગઈ. કોઈ ક્રિયાની જરૂર નથી.'),
('education', 'Visit example.com/help for the public documentation.', 'सार्वजनिक जानकारी के लिए example.com/help देखें।', 'જાહેર માહિતી માટે example.com/help જુઓ.'),
('personal', 'Please bring a notebook to class tomorrow.', 'कृपया कल कक्षा में नोटबुक लेकर आएँ।', 'કૃપા કરીને કાલે વર્ગમાં નોટબુક લાવજો.'),
('bank_reminder', 'Check your statement using your usual bank app.', 'अपने नियमित बैंक ऐप से अपना स्टेटमेंट देखें।', 'તમારી નિયમિત બેંક ઍપથી તમારું સ્ટેટમેન્ટ જુઓ.'),
]
def build():
    cases=[]
    for positive, rows in ((True, scams), (False, benign)):
        for index,(category,*translations) in enumerate(rows):
            for language,text in zip(('en','hi','gu'),translations):
                cases.append(dict(id=f'{category}-{index}-{language}-{int(positive)}', language=language, category=category,
                    input_type='text', content=text, label=int(positive), source_group=f'authored-{category}-{index}-{int(positive)}',
                    source='SafeX AI authored development fixture', expected='WARN' if positive else 'ALLOW'))
    for index,content in enumerate(['WIFI:T:WPA;S:Fixture;P:Test;;','BEGIN:VCARD\nN:Fixture\nEND:VCARD','mailto:test@example.com','smsto:123:hello','tel:123','intent://fixture']):
        cases.append(dict(id=f'unsupported-qr-{index}', language='en', category='action_qr', input_type='qr', content=content,
            label=0, expected='UNSUPPORTED', source_group=f'qr-{index}', source='SafeX AI authored development fixture'))
    for index,content in enumerate(['hxxps://paypal-secure[.]example/verify', 'https://paypal.com@other.example/login', 'paypal-secure.example/verify', 'https://pay\u200Bpal-secure.example/verify']):
        cases.append(dict(id=f'visible-link-{index}', language='en', category='link_normalization', input_type='text', content=content,
            label=1, expected='WARN', source_group=f'link-{index}', source='SafeX AI authored development fixture'))
    return dict(schema_version=1, kind='Authored development functional corpus; not independent accuracy evidence', cases=cases)
if __name__=='__main__':
    output=ROOT/'docs/test-results/floating-review-corpus.json'
    output.write_text(json.dumps(build(),ensure_ascii=False,indent=2)+'\n')
    print(f'{len(build()["cases"])} authored functional cases written to {output}')
