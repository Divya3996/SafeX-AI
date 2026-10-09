"""Bounded, identifier-masked features shared with TextModelFeatures.kt (v3)."""
import re
import unicodedata

FEATURE_VERSION = 'unicode_word_bigram_char3_masked_v1'
MAX_TEXT = 12000
MAX_WORDS = 512

def normalize(text):
    text = unicodedata.normalize('NFKC', text[:MAX_TEXT]).lower()
    text = re.sub('[\u200b-\u200d\ufeff]', '', text)
    text = re.sub(r'(?i)\b(?:https?|hxxps?)://\S+|\bwww\.\S+', ' urltoken ', text)
    text = re.sub(r'[\w.+-]+@[\w.-]+\.[a-z]{2,}', ' emailtoken ', text)
    text = re.sub(r'\d+', ' numbertoken ', text)
    return text

def words(text):
    found, word = [], ''
    for char in normalize(text):
        if unicodedata.category(char)[0] in 'LMN' or char == '_':
            word += char
        else:
            if len(word) >= 2:
                found.append(word)
                if len(found) == MAX_WORDS:
                    return found
            word = ''
    if len(word) >= 2 and len(found) < MAX_WORDS:
        found.append(word)
    return found

def features(text):
    tokens = words(text)
    result = set(tokens)
    result.update(a + ' ' + b for a, b in zip(tokens, tokens[1:]))
    for word in tokens:
        if 3 <= len(word) <= 32:
            padded = '^' + word + '$'
            result.update('~' + padded[i:i+3] for i in range(len(padded)-2))
    return result

def campaign_template(text):
    # Removes numeric, URL, email and punctuation variants before grouping.
    return ' '.join(words(text))
