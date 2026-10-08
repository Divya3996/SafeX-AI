#!/usr/bin/env python3
"""Train an offline binary n-gram logistic classifier on authored synthetic examples.
Requires numpy. This small prototype is NOT a real-world accuracy benchmark.
Training and held-out message templates are kept separate.
"""
import json, re, unicodedata, hashlib
from pathlib import Path
import numpy as np
ROOT=Path(__file__).resolve().parents[1]
SCAMS=[
'Bank support urgent share your OTP to avoid account suspension',
'Please send your password immediately to restore your account',
'Enter your security code and bank details to verify your KYC',
'Your bank account is blocked submit your OTP now',
'You have won a lottery pay the processing fee to claim your prize',
'Congratulations winner transfer money to claim your reward',
'Get a guaranteed loan deposit the registration fee first',
'Work from home job pay an advance fee to start earning',
'Your refund is ready scan this QR and enter your UPI PIN to receive money',
'Police digital arrest transfer payment now and do not tell anyone',
'Install anydesk and send me your bank login password',
'Government income tax refund enter your card number and CVV',
'Your parcel cannot be delivered pay a small customs fee now',
'I am your manager urgently buy gift cards and send the codes',
'Tell me your verification code to confirm your identity',
'Free gift offer send your bank details and password to claim',
'Your account will be suspended verify your login at https://verify-bank.example',
'Investment guaranteed double returns transfer to my wallet now',
'Send the one time code you received so I can unlock your account',
'Pay a security deposit to get this guaranteed job offer',
'Account hacked give us your PIN immediately for security verification',
'You won cashback enter your UPI PIN on this link',
'Loan approved pay a processing fee before disbursement',
'Bank verification send OTP jaldi account block ho jayega',
'Aap lottery jeet gaye prize ke liye paise bhejo',
'बैंक खाता बंद होगा तुरंत अपना ओटीपी भेजो',
'इनाम पाने के लिए पैसे भेजो अभी',
'आपकी नौकरी के लिए पहले registration fee pay करो',
'KYC update urgent provide your account password',
'Delivery support asks you to confirm your card details on this unfamiliar link',
]
BENIGN=[
'Never share your OTP password or PIN with anyone',
'Do not send your password to bank support',
'Your OTP is 123456 do not share it with anyone',
'Your account payment was successful thank you',
'Bank transaction completed balance updated',
'Your refund has been credited to your bank account',
'Your package will arrive tomorrow track delivery in the official app',
'Your meeting is urgent please join the office call',
'Please verify the report and send it to the team',
'We are hiring apply on our company careers page no fees required',
'Your job interview is scheduled for tomorrow',
'Your loan statement is available in the banking app',
'Limited offer on shoes at the local store',
'You earned cashback on your recent purchase',
'Use a strong password and enable two factor authentication',
'The police advise you not to transfer money to unknown callers',
'Beware of lottery scams never pay to claim a prize',
'Please pay the electricity bill through the official utility app',
'I sent you the payment check your balance',
'The bank never asks for your PIN or security code',
'Do not scan QR codes to receive refunds',
'Please share your project notes with the study group',
'Your security code expires in five minutes do not disclose it',
'Congratulations on winning the school competition',
'We received your registration for the free workshop',
'Apna OTP kabhi share mat karo bank password nahi mangta',
'अपना ओटीपी किसी को न बताएं',
'आपका भुगतान सफल हुआ धन्यवाद',
'See the official guide at https://support.example about account security',
'Your delivery address has been updated as requested',
]
# Authored native-script examples; equal scam/benign counts for each added language.
SCAMS += [
'तुरंत अपना ओटीपी और पासवर्ड भेजें वरना बैंक खाता बंद होगा',
'केवाईसी के लिए अपना पिन और गोपनीय कोड बताएं',
'आप लॉटरी जीते हैं इनाम पाने के लिए पहले शुल्क जमा करें',
'नौकरी पक्की है पहले पंजीकरण फीस भुगतान करें',
'रिफंड प्राप्त करने के लिए क्यूआर स्कैन करें और यूपीआई पिन डालें',
'पुलिस बोल रही है डिजिटल गिरफ्तारी से बचने के लिए पैसे भेजें',
'बैंक सहायता को अपना पासवर्ड बताइए अभी',
'सरकारी पुरस्कार जीत गए पुरस्कार पाने के लिए रुपये भेजें',
'लोन मंजूर है पहले प्रोसेसिंग शुल्क जमा करें',
'अपना सुरक्षा कोड साझा करें बैंक विवरण दें तुरंत',
'બેંક સહાય હમણાં તમારો ઓટીપી અને પાસવર્ડ મોકલો',
'કેવાયસી માટે તમારો પિન અને સુરક્ષા કોડ આપો',
'તમે લોટરી જીત્યા છો ઇનામ મેળવવા માટે પહેલા ફી ચૂકવો',
'નોકરીની ગેરંટી છે પહેલા રજિસ્ટ્રેશન ફી જમા કરો',
'રિફંડ મેળવવા માટે ક્યુઆર સ્કેન કરો અને યુપીઆઈ પિન દાખલ કરો',
'પોલીસ બોલે છે ડિજિટલ અરેસ્ટથી બચવા હમણાં પૈસા મોકલો',
'તમારું ખાતું બંધ થશે તરત બેંકને પાસવર્ડ જણાવો',
'સરકારી ઇનામ જીત્યા છો પૈસા મોકલો અને ઇનામ મેળવો',
'લોન મંજૂર છે પહેલા પ્રોસેસિંગ ફી ચૂકવો',
'તાત્કાલિક તમારો ગુપ્ત કોડ શેર કરો બેંકની વિગતો મોકલો',
]
BENIGN += [
'अपना ओटीपी या पासवर्ड किसी को न बताएं',
'बैंक कभी आपका पिन नहीं माँगता गोपनीय कोड न दें',
'आपका भुगतान सफल हुआ धन्यवाद',
'आपकी नौकरी का साक्षात्कार सोमवार को है कोई शुल्क नहीं है',
'रिफंड आपके खाते में जमा हो गया है',
'पुलिस अनजान लोगों को पैसे भेजने से बचने की सलाह देती है',
'सुरक्षा के लिए पासवर्ड किसी को न भेजें',
'स्कूल के पुरस्कार समारोह में आपका स्वागत है',
'आपके लोन का विवरण आधिकारिक बैंक ऐप में उपलब्ध है',
'तुरंत कार्यालय की बैठक में शामिल हों',
'તમારો ઓટીપી કે પાસવર્ડ ક્યારેય કોઈને આપશો નહીં',
'બેંક તમારો પિન માગતી નથી સુરક્ષા કોડ શેર ન કરો',
'તમારી ચુકવણી સફળ થઈ આભાર',
'નોકરીનો ઇન્ટરવ્યૂ સોમવારે છે કોઈ ફી નથી',
'રિફંડ તમારા ખાતામાં જમા થઈ ગયું છે',
'પોલીસ અજાણ્યા વ્યક્તિને પૈસા ન મોકલવાની સલાહ આપે છે',
'સુરક્ષા માટે પાસવર્ડ કોઈને મોકલશો નહીં',
'શાળાના ઇનામ વિતરણ સમારોહમાં તમારું સ્વાગત છે',
'લોનનું સ્ટેટમેન્ટ બેંકની સત્તાવાર એપમાં ઉપલબ્ધ છે',
'હમણાં ઓફિસની બેઠકમાં જોડાઓ',
]
HELDOUT=[
('To prevent suspension urgently send your bank password to support',1),
('You are the lucky winner deposit a fee and claim your lottery reward',1),
('Guaranteed employment available pay the joining fee first',1),
('A refund is pending enter your UPI PIN and scan to receive money',1),
('Customs police require an immediate transfer keep this secret',1),
('Provide the verification code to recover your locked account',1),
('Send your CVV and card details for an income tax refund',1),
('Loan offer confirmed pay processing charges in advance',1),
('Enter your bank login at https://secure-verify.example immediately',1),
('To claim a free gift submit your account password',1),
('Please send the OTP immediately for KYC approval',1),
('Install remote access software and reveal your banking PIN',1),
('Never give anyone your bank password or OTP',0),
('Your payment has been received and refund credited',0),
('The next job interview will take place on Friday',0),
('The official banking app contains your monthly statement',0),
('Please share the meeting notes after the office call',0),
('Our security team says do not reveal your verification code',0),
('School prize winners are invited to the ceremony',0),
('Your parcel is scheduled to arrive this evening',0),
('We do not charge a registration fee for this workshop',0),
('UPI payment completed successfully thank you',0),
('There is an urgent issue with the project report',0),
('Beware of anyone asking you to send an OTP for a refund',0),
]
HELDOUT += [
('खाता निलंबित होगा अभी अपना ओटीपी बताइए', 1),
('इनाम प्राप्त करने से पहले पैसे जमा करें', 1),
('लोन के लिए पहले पंजीकरण शुल्क भुगतान करें', 1),
('कभी अपना पासवर्ड साझा न करें', 0),
('आपका बैंक भुगतान पूरा हुआ', 0),
('कल नौकरी का इंटरव्यू है', 0),
('ખાતું બંધ થશે હમણાં તમારો ઓટીપી શેર કરો', 1),
('લોટરીનું ઇનામ લેવા પહેલા પૈસા ચૂકવો', 1),
('નોકરી માટે પહેલા ફી મોકલો', 1),
('ઓટીપી ક્યારેય શેર ન કરો', 0),
('બેંકમાં તમારી ચુકવણી પૂર્ણ થઈ', 0),
('નોકરીનો ઇન્ટરવ્યૂ કાલે છે', 0),
]
def tokens(t):
 t=unicodedata.normalize('NFKC',t).lower()
 t=re.sub('[\u200B-\u200D\uFEFF]', '', t)
 # Combining marks belong to native-script words; match Android's L/M/N tokenizer.
 words=[]; word=''
 for char in t:
  if unicodedata.category(char)[0] in 'LMN' or char=='_': word+=char
  else:
   if len(word)>=2: words.append(word)
   word=''
 if len(word)>=2: words.append(word)
 return words
def features(t):
 ts=tokens(t);return set(ts+[' '.join(ts[i:i+2]) for i in range(len(ts)-1)])
examples=[(s,y) for y,ss in [(1,SCAMS),(0,BENIGN)] for s in ss]
vocab=sorted(set().union(*(features(s) for s,y in examples)))
index={w:i for i,w in enumerate(vocab)}
def vector(t):
 a=np.zeros(len(vocab))
 for term in features(t):
  if term in index:a[index[term]]=1
 return a
X=np.array([vector(t) for t,y in examples]);y=np.array([y for t,y in examples])
w=np.zeros(len(vocab));b=0.
for _ in range(2400):
 p=1/(1+np.exp(-np.clip(X@w+b,-30,30)))
 w-=.2*(X.T@(p-y)/len(y)+.025*w)
 b-=.2*np.mean(p-y)
model={'version':'synthetic-text-v2','training_scope':'Authored synthetic English, Hindi, Gujarati and Hinglish prototype; not a real-world benchmark','token_pattern':'unicode_letter_mark_digit_underscore_min2','vocabulary':vocab,'weights':w.round(8).tolist(),'intercept':round(b,8),'threshold':.60}
asset=ROOT/'android-app/app/src/main/assets/text-model.json';asset.write_text(json.dumps(model,ensure_ascii=False,indent=2)+'\n')
out=[]
for t,label in HELDOUT:
 prob=float(1/(1+np.exp(-(vector(t)@w+b))));out.append({'text':t,'label':label,'probability':round(prob,4),'prediction':int(prob>=.6)})
tp=sum(a['prediction']==1 and a['label']==1 for a in out);fp=sum(a['prediction']==1 and a['label']==0 for a in out);fn=sum(a['prediction']==0 and a['label']==1 for a in out);tn=sum(a['prediction']==0 and a['label']==0 for a in out)
report={'scope':'36 authored synthetic validation messages used to select the warning threshold. NOT an independent or real-world benchmark.','train_count':len(examples),'test_count':len(out),'threshold':.6,'confusion_matrix':{'tp':tp,'fp':fp,'fn':fn,'tn':tn},'precision':tp/max(1,tp+fp),'recall':tp/max(1,tp+fn),'sha256':hashlib.sha256(asset.read_bytes()).hexdigest(),'examples':out}
(ROOT/'models/text-evaluation.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
(ROOT/'models/text-training.json').write_text(json.dumps([{'text':s,'label':y,'source':'authored synthetic'} for s,y in examples],ensure_ascii=False,indent=2)+'\n')
print(json.dumps({k:v for k,v in report.items() if k!='examples'},indent=2))
