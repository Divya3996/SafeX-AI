import type { Evidence, TextModel } from "./contracts";

export function normalize(text: string): string {
  return text
    .normalize("NFKC")
    .replace(/[\u200B-\u200D\uFEFF]/g, "")
    .toLowerCase();
}
export function textTokens(text: string): string[] {
  const masked = normalize(text.slice(0, 12000))
    .replace(/\b(?:https?|hxxps?):\/\/\S+|\bwww\.\S+/gi, " urltoken ")
    .replace(
      /[\p{L}\p{M}\p{N}_.+-]+@[\p{L}\p{M}\p{N}_.-]+\.[a-z]{2,}/gu,
      " emailtoken ",
    )
    .replace(/\p{Nd}+/gu, " numbertoken ");
  return (masked.match(/[\p{L}\p{M}\p{N}_]{2,}/gu) ?? []).slice(0, 512);
}
export function textFeatures(text: string): Set<string> {
  const tokens = textTokens(text),
    terms = new Set(tokens);
  for (let i = 1; i < tokens.length; i++)
    terms.add(`${tokens[i - 1]} ${tokens[i]}`);
  for (const token of tokens)
    if (token.length >= 3 && token.length <= 32) {
      const padded = `^${token}$`;
      for (let i = 0; i < padded.length - 2; i++)
        terms.add(`~${padded.slice(i, i + 3)}`);
    }
  return terms;
}
export class TextClassifier {
  readonly weights: Map<string, number>;
  constructor(readonly model: TextModel) {
    if (
      model.feature_version !== "unicode_word_bigram_char3_masked_v1" ||
      model.vocabulary.length !== model.weights.length ||
      model.vocabulary.length > 16000 ||
      !Number.isFinite(model.intercept) ||
      !model.weights.every(Number.isFinite)
    )
      throw Error("modelInvalid");
    this.weights = new Map(
      model.vocabulary.map((term, i) => [term, model.weights[i]]),
    );
  }
  predict(text: string): number {
    if (!text.trim()) return 0;
    let sum = this.model.intercept;
    for (const term of textFeatures(text)) sum += this.weights.get(term) ?? 0;
    return 1 / (1 + Math.exp(-Math.max(-30, Math.min(30, sum))));
  }
  covered(text: string): boolean {
    const tokens = [...new Set(textTokens(text))].filter(
      (t) => !["urltoken", "emailtoken", "numbertoken"].includes(t),
    );
    const letters = tokens.join("").match(/\p{L}/gu) ?? [];
    return (
      tokens.length >= 4 &&
      letters.length > 0 &&
      letters.filter((c) => /^[a-z]$/.test(c)).length / letters.length >=
        0.95 &&
      tokens.filter((t) => this.weights.has(t)).length / tokens.length >= 0.6
    );
  }
}

// Negated safety advice is removed before applying action rules or the text model.
export function activeText(text: string): string {
  return normalize(text)
    .split(
      /[.!?।॥\n]+\s*|\b(?:but|however|and)\b|लेकिन|परंतु|मगर| और |પરંતુ| અને /u,
    )
    .filter((part) => {
      const negative =
        /\b(never|do not|don't)\s+(share|send|reveal|disclose|give|tell|provide|submit|confirm|scan|pay|transfer|install|enter|click|tap|open|visit|follow|reply|call)\b|^\s*beware of|कभी.*नहीं|कभी.*(न बत|न दें|न भेज|साझा न|शेयर न)|मत (बत|भेज|दे|शेयर|साझा|स्कैन|स्केन|कर|भुगतान|क्लिक|खोल)|(?:^|\s)न\s+(बत|दें|भेज|करें|करे|शेयर|डाल|दर्ज|भर)|साझा न|शेयर न|क्लिक न|बताएं नहीं|भेजें नहीं|ઓટીપી.*(શેર ન|આપશો નહીં|જણાવશો નહીં)|ક્યારેય.*(ન કરો|ન આપ|ન મોકલ|ન જણાવ|ન શેર)|(?:^|\s)(?:ન|ના)\s+(મોકલ|જણાવ|આપ|નાખ|દાખલ|શેર|ખોલ|ક્લિક|ભર|ચુકવ)|શેર (ન|ના) કરો|આપશો નહીં|મોકલશો નહીં|જણાવશો નહીં|ખોલશો નહીં|(સ્કેન|સ્કૅન|ચુકવણી|ઇન્સ્ટોલ|ક્લિક).*ન કરો|સાવધાન રહો|सावधान रहें/u.test(
          part,
        );
      const exception =
        /\b(but|except|however|instead)\b|लेकिन|परंतु|मगर|लेकिन अभी|પરંતુ|પણ હમણાં|પણ હવે/u.test(
          part,
        );
      const secrecy =
        /do not tell (anyone|your family)|किसी को मत बता|કોઈને (કહેશો|જણાવશો) નહીં/u.test(
          part,
        ) &&
        /pay|transfer|send|money|bank|police|पैसे|भुगतान|बैंक|पुलिस|પૈસા|ચુકવણી|બેંક|પોલીસ/u.test(
          part,
        );
      return !negative || exception || secrecy;
    })
    .join(". ");
}
export function messageSignals(
  text: string,
  hasLinks: boolean,
): { active: string; reasons: Evidence[]; eligible: boolean } {
  const active = activeText(text),
    reasons: Evidence[] = [];
  const has = (r: RegExp) => r.test(active);
  const urgency = has(
    /\b(urgent|immediately|act now|suspended|blocked|within \d+ (minutes|hours)|last chance)\b|तुरंत|अभी|जल्दी|बंद हो|निलंबित|अवरुद्ध|तत्काल|છેલ્લી તક|હમણાં|તાત્કાલિક|તરત|તુરંત|બંધ થશે|બંધ થઈ|બ્લોક|jaldi/u,
  );
  const credentials = has(
    /\b(otp|password|pin|verification code|security code|bank details|card number|card details|cvv|passcode)\b|ओटीपी|पासवर्ड|पिन|गोपनीय कोड|सुरक्षा कोड|सत्यापन कोड|बैंक विवरण|कार्ड नंबर|सीवीवी|ઓટીપી|પાસવર્ડ|પિન|ગુપ્ત કોડ|સુરક્ષા કોડ|ચકાસણી કોડ|બેંકની વિગતો|કાર્ડ નંબર|સીવીવી/u,
  );
  const request = has(
    /\b(share|send|tell|provide|enter|reveal|confirm|submit|give)\b|भेज|बताओ|बताएं|बताइए|दें|दीजिए|डालें|दर्ज|साझा|शेयर|पुष्टि|મોકલ|જણાવ|આપો|દાખલ|શેર|પુષ્ટિ|ભરો|bhejo|batao/u,
  );
  const money = has(
    /\b(pay|transfer|deposit|fee|payment|upi|refund|wallet|bank|rupees|rs|fine|toll|bill)\b|₹|पैसे|भुगतान|रुपये|शुल्क|जमा|रिफंड|यूपीआई|बैंक|बिल|जुर्माना|પૈસા|પૈસો|ચુકવણી|રૂપિયા|ફી|જમા|રિફંડ|યુપીઆઈ|યુપી[આઇઈ]+|બેંક|બિલ|દંડ/u,
  );
  const action = has(
    /\b(pay|transfer|deposit|withdraw|buy|purchase)\b|जमा करें|भुगतान करें|भरें|पैसे भेज|चुकाएं|ચુકવો|જમા કરો|ભરો|પૈસા મોકલ/u,
  );
  const prize = has(
    /\b(won|winner|lottery|prize|jackpot|free gift|reward|cashback)\b|इनाम|लॉटरी|पुरस्कार|जीत गए|जीता|कैशबैक|ઇનામ|લોટરી|પુરસ્કાર|જીત્યા|જીત્યું|કેશબેક/u,
  );
  const jobs = has(
    /\b(job|hiring|salary|work from home|part time|loan|approved loan)\b|नौकरी|ऋण|कर्ज|लोन|वेतन|નોકરી|લોન|પગાર|ઘરેથી કામ/u,
  );
  const authority = has(
    /\b(bank|police|government|customs|income tax|support team|rbi|sbi|hdfc|icici|aadhaar|kyc)\b|बैंक|पुलिस|सरकार|कस्टम|आधार|केवाईसी|બેંક|પોલીસ|સરકાર|કસ્ટમ|આધાર|કેવાયસી/u,
  );
  const add = (id: string, weight: number) => reasons.push({ id, weight });
  if (credentials && request) add("credential_request", 70);
  if (urgency) add("urgency", 12);
  if (authority && ((credentials && request) || (urgency && hasLinks)))
    add("authority", 18);
  if (prize && (money || request || hasLinks)) add("prize", 55);
  if (
    jobs &&
    has(
      /\b(fee|deposit|pay first|registration|processing|guaranteed|advance)\b|फीस|शुल्क|पहले.*(पैसे|भुगतान|जमा)|पंजीकरण|गारंटी|પ્રથમ.*(ચૂકવ|ચુકવ|જમા)|પહેલા.*(પૈસા|ચૂકવ|ચુકવ|જમા)|ફી|રજિસ્ટ્રેશન|ગેરંટી/u,
    )
  )
    add("upfront_fee", 60);
  if (
    money &&
    action &&
    has(
      /\b(guaranteed|risk[- ]free|double your money)\b|मुनाफे की गारंटी|निश्चित मुनाफा|નફાની ગેરંટી/u,
    ) &&
    has(
      /\b(profit|returns|investment|crypto)\b|मुनाफा|मुनाफे|निवेश|क्रिप्टो|નફો|નફાની|રોકાણ|ક્રિપ્ટો/u,
    )
  )
    add("investment", 35);
  if (
    money &&
    has(
      /\b(scan.*(receive|refund)|(receive|refund).*scan|upi pin.*receive|receive.*upi pin)\b|(स्कैन|स्केन|क्यूआर|यूपीआई पिन).*(पाने|प्राप्त|रिफंड|वापस)|(पाने|प्राप्त|रिफंड|वापस).*(स्कैन|स्केन|क्यूआर|यूपीआई पिन)|(સ્કેન|સ્કૅન|ક્યુઆર|પિન).*(મેળવ|રિફંડ)|(મેળવ|રિફંડ).*(સ્કેન|સ્કૅન|ક્યુઆર|પિન)/u,
    )
  )
    add("upi_receive", 85);
  if (
    has(
      /\b(digital arrest|arrest warrant|keep.*secret|do not tell|remote access|anydesk|teamviewer)\b|डिजिटल अरेस्ट|गिरफ्तारी|किसी को मत बता|डिजिटल गिरफ्तारी|દિજિટલ અરેસ્ટ|ડિજિટલ અરેસ્ટ|ધરપકડ|કોઈને કહેશો નહીં|કોઈને જણાવશો નહીં|એનીડેસ્ક/u,
    ) &&
    (money || authority || request)
  )
    add("coercion", 75);
  if (money && urgency && request && !reasons.some((r) => r.weight >= 35))
    add("financial_pressure", 35);
  if (hasLinks && urgency) add("urgent_link", 12);
  const webAction = has(
    /\b(click|tap|visit|open|follow|login|log in|sign in|update|validate|activate|claim|redeem)\b|क्लिक|टैप|लिंक खोल|लॉगिन|लॉग इन|अपडेट|सत्यापन|દબાવો|ક્લિક|લિંક ખોલ|લોગિન|અપડેટ|ચકાસણી/u,
  );
  const callback = has(
    /\b(call|reply|contact|text us)\b|कॉल|जवाब दें|संपर्क करें|કોલ|જવાબ આપો|સંપર્ક કરો/u,
  );
  const giftAction =
    has(
      /\bgift cards?\b|redemption codes?|गिफ्ट कार्ड|રિડેમ્પશન કોડ|ગિફ્ટ કાર્ડ/u,
    ) &&
    has(
      /\b(buy|purchase|send|share|give)\b|खरीद|भेज|साझा|खरीदो|ખરીદ|મોકલ|શેર/u,
    );
  return {
    active,
    reasons,
    eligible:
      ((credentials || money || prize || jobs || authority) &&
        (request || urgency || webAction || callback || action)) ||
      (webAction && hasLinks) ||
      giftAction,
  };
}
