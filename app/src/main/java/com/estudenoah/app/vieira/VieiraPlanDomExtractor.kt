package com.estudenoah.app.vieira

internal object VieiraPlanDomExtractor {
    const val PLAN_URL = "https://portal.asav.org.br/frameHTML/web/app/edu/PortalEducacional/#/plano-aula"

    // Executado somente dentro da WebView autenticada. O resultado contém campos visíveis,
    // nunca cookies, storage, credenciais ou headers da sessão.
    val EXTRACTION_SCRIPT: String = """
        (function() {
          const clean = value => (value || '').replace(/\u00a0/g, ' ').replace(/[\t ]+/g, ' ').trim();
          const fold = value => clean(value).normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
          const labels = {
            subject: ['disciplina'], classGroup: ['turma'], lessonType: ['tipo'],
            startTime: ['inicio', 'início'], endTime: ['termino', 'término'],
            plannedContent: ['conteudo previsto', 'conteúdo previsto'],
            completedContent: ['conteudo realizado', 'conteúdo realizado'],
            homework: ['licao de casa', 'lição de casa']
          };
          const allLabels = Object.values(labels).flat().map(fold);
          const visible = element => !!(element.offsetWidth || element.offsetHeight || element.getClientRects().length);
          const roots = [document];
          Array.from(document.querySelectorAll('iframe')).forEach(frame => {
            try { if (frame.contentDocument) roots.push(frame.contentDocument); } catch (_) { /* origem diferente: ignorar */ }
          });
          function field(lines, names) {
            const targets = names.map(fold);
            for (let index = 0; index < lines.length; index++) {
              const line = clean(lines[index]); const normalized = fold(line);
              for (const target of targets) {
                if (normalized === target) {
                  const next = clean(lines[index + 1]);
                  return next && !allLabels.includes(fold(next)) ? next : null;
                }
                if (normalized.startsWith(target + ':')) return clean(line.substring(line.indexOf(':') + 1)) || null;
              }
            }
            return null;
          }
          const candidates = roots.flatMap(root => Array.from(root.querySelectorAll('article,section,li,tr,div,mat-card')))
            .filter(visible)
            .filter(element => /Disciplina/i.test(element.innerText || '') &&
              /(Início|Inicio|Término|Termino|Conteúdo previsto|Conteudo previsto)/i.test(element.innerText || ''));
          const blocks = candidates.filter(element => !Array.from(element.children).some(child =>
            /Disciplina/i.test(child.innerText || '') &&
            /(Início|Inicio|Término|Termino|Conteúdo previsto|Conteudo previsto)/i.test(child.innerText || '')));
          const classes = blocks.map(element => {
            const lines = (element.innerText || '').split(/\n+/).map(clean).filter(Boolean);
            const lesson = (element.innerText || '').match(/\bAula\s*(\d+)/i);
            return {
              lessonNumber: lesson ? Number(lesson[1]) : null,
              subject: field(lines, labels.subject), classGroup: field(lines, labels.classGroup),
              lessonType: field(lines, labels.lessonType), startTime: field(lines, labels.startTime),
              endTime: field(lines, labels.endTime), plannedContent: field(lines, labels.plannedContent),
              completedContent: field(lines, labels.completedContent), homework: field(lines, labels.homework)
            };
          }).filter(item => item.subject || item.startTime || item.completedContent || item.plannedContent);
          const dateInputs = roots.flatMap(root => Array.from(root.querySelectorAll('input'))).filter(visible);
          let dateText = '';
          for (const input of dateInputs) {
            const context = fold((input.getAttribute('aria-label') || '') + ' ' + (input.placeholder || '') + ' ' + (input.name || ''));
            if (context.includes('data') || context.includes('plano')) { dateText = clean(input.value); if (dateText) break; }
          }
          if (!dateText) {
            const pageText = roots.map(root => root.body ? root.body.innerText : '').join('\n');
            const match = pageText.match(/\b(\d{2})\/(\d{2})\/(\d{4})\b/);
            if (match) dateText = match[0];
          }
          const match = dateText.match(/(\d{2})[\/-](\d{2})[\/-](\d{4})/);
          const isoMatch = dateText.match(/(\d{4})-(\d{2})-(\d{2})/);
          const date = match ? match[3] + '-' + match[2] + '-' + match[1] :
            (isoMatch ? isoMatch[1] + '-' + isoMatch[2] + '-' + isoMatch[3] : null);
          return JSON.stringify({date: date, classes: classes});
        })();
    """.trimIndent()
}
