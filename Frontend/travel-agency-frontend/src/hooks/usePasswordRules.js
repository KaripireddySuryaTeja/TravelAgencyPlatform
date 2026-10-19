export function evaluatePasswordRules(password, firstName, email) {
  const pw = password || '';
  const pwLower = pw.toLowerCase();
  const first = (firstName || '').trim().toLowerCase();
  const localPart = ((email || '').split('@')[0] || '').toLowerCase();

  const containsFirst = first.length > 0 && pwLower.includes(first);
  const containsLocal = localPart.length > 0 && pwLower.includes(localPart);

  return {
    capital: /[A-Z]/.test(pw),
    special: /[^A-Za-z0-9]/.test(pw),
    number: /[0-9]/.test(pw),
    length: pw.length >= 8,
    notIdentity: pw.length > 0 && !containsFirst && !containsLocal,
  };
}

export function allRulesPass(rules) {
  return rules.capital && rules.special && rules.number && rules.length && rules.notIdentity;
}
