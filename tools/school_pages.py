#!/usr/bin/env python3
"""Renders the teacher one-pager in every UI language into app/src/wasmJsMain/resources.

    python3 tools/school_pages.py

skole.html is the Danish page (its link is already in circulation), the others are school-<lang>.html
with English at school.html. Edit the TEXT table below, never the generated files.
"""
from pathlib import Path

OUT = Path(__file__).resolve().parent.parent / "app/src/wasmJsMain/resources"

PAGES = {"en": "school.html", "da": "skole.html", "ro": "school-ro.html", "ru": "school-ru.html", "uk": "school-uk.html"}
LABELS = {"en": "English", "da": "Dansk", "ro": "Română", "ru": "Русский", "uk": "Українська"}

TEXT = {
"en": dict(
    title="CodeTrail in the classroom",
    lead="A free game where pupils program a little hero to the goal with cards. No login, no installation, no data collection. The language follows the device and can be set under <em>Settings</em>.",
    cta="Open the game: codetrail.dk",
    facts=[("Age group", "Grades 0-4 (the first two difficulty tiers from about age 5)"),
           ("Subjects", "Computing / digital literacy, mathematics, enrichment lessons"),
           ("Time", "20-45 minutes per session, repeatable (levels are always new)"),
           ("Devices", "Chromebook, PC, Mac or a recent iPad in the browser. Works with mouse and touch."),
           ("Price", "Free, no ads")],
    learn_h="What do pupils learn?",
    learn=[("Algorithmic thinking:", "breaking a task into small steps in the right order."),
           ("Directions and perspective:", "first up/down/left/right, later \"turn left\" as seen by the hero."),
           ("Debugging:", "the program runs, the hero goes wrong, the pupil finds and fixes the mistake."),
           ("Loops and blocks:", "\"repeat 3 times\" and a reusable \"block A\" on the last two tiers."),
           ("Optimisation:", "more stars for fewer cards, so pupils look for the shortest solution.")],
    tiers_h="Difficulty tiers",
    tiers=[("1 First steps", "Arrows up, down, right, left. Short paths."),
           ("2 Longer paths", "Same arrows, longer paths and more cards to keep track of."),
           ("3 Turn and go", "\"Forward X\" and turn left / right as seen by the hero."),
           ("4 Jump over", "Hop over water and rocks."),
           ("5 Loops", "\"Repeat ×N\" with cards inside. Bigger boards."),
           ("6 Blocks", "Build \"block A\" once and call it several times.")],
    modes="Two modes: <strong>Build the path</strong> (the pupil writes the program) and <strong>Guess the finish</strong> (the pupil reads a finished program and points at where the hero ends). The second one trains reading code.",
    lesson_h="Running a lesson",
    lesson=["Show the game on the board for 3 minutes: create a profile, pick \"First steps\", solve one level together.",
            "Pupils open <strong>codetrail.dk</strong>, type their name as a profile and pick the same tier.",
            "15-20 minutes on their own. The <strong>?</strong> badge on the card tray explains the cards, the bulb gives a hint.",
            "Finish by asking: \"Who solved a level with the fewest cards? How?\"",
            "Next time: one tier up. \"Guess the finish\" works well as a whole-class task on the board."],
    practical_h="Practical notes",
    practical=["Progress is saved in the browser on each device. If pupils share devices, a profile can be exported as a code under <em>Profiles</em> and imported elsewhere.",
               "No accounts, no e-mail, nothing leaves the device. More: <a href=\"privacy.html\">privacy policy</a>.",
               "Also available as an app for Windows, macOS, Linux and Android: <a href=\"https://github.com/acidsnare/CodeTrail/releases\">github.com/acidsnare/CodeTrail/releases</a>.",
               "Languages under <em>Settings</em>: Danish, English, Romanian, Russian, Ukrainian."],
    contact_h="Contact",
    contact="The game is made by a parent in his spare time. Questions, wishes and bugs: write, or open an issue on <a href=\"https://github.com/acidsnare/CodeTrail/issues\">GitHub</a>.",
),
"da": dict(
    title="CodeTrail i klassen",
    lead="Et gratis spil, hvor eleverne programmerer en lille helt frem til målet med kort. Ingen login, ingen installation, ingen indsamling af data. Sproget følger enheden og kan sættes til dansk under <em>Indstillinger</em>.",
    cta="Åbn spillet: codetrail.dk",
    facts=[("Målgruppe", "0.-4. klasse (de to første sværhedsgrader fra ca. 5 år)"),
           ("Fag", "Teknologiforståelse, matematik, understøttende undervisning"),
           ("Tid", "20-45 minutter pr. gang, kan gentages (banerne er altid nye)"),
           ("Udstyr", "Chromebook, pc, Mac eller nyere iPad i browseren. Virker med mus og touch."),
           ("Pris", "Gratis, uden reklamer")],
    learn_h="Hvad lærer eleverne?",
    learn=[("Algoritmisk tænkning:", "dele en opgave op i små trin i den rigtige rækkefølge."),
           ("Retninger og perspektiv:", "først op/ned/højre/venstre, senere \"drej til venstre\" set fra heltens side."),
           ("Fejlfinding:", "programmet køres, helten går forkert, eleven finder fejlen og retter den."),
           ("Løkker og blokke:", "\"gentag 3 gange\" og en genbrugelig \"blok A\" på de to sidste sværhedsgrader."),
           ("Optimering:", "flere stjerner for færre kort, så eleverne leder efter den korteste løsning.")],
    tiers_h="Sværhedsgrader",
    tiers=[("1 Første skridt", "Pile op, ned, højre, venstre. Korte veje."),
           ("2 Længere veje", "Samme pile, længere veje og flere kort at holde styr på."),
           ("3 Drej og gå", "\"Gå frem X\" og drej til venstre / højre set fra heltens side."),
           ("4 Hop over", "Spring over vand og sten."),
           ("5 Løkker", "\"Gentag ×N\" med kort indeni. Større baner."),
           ("6 Blokke", "Byg \"blok A\" én gang og kald den flere gange.")],
    modes="To spilformer: <strong>Byg vejen</strong> (eleven skriver programmet) og <strong>Gæt målet</strong> (eleven læser et færdigt program og peger på, hvor helten ender). Den sidste træner læsning af kode.",
    lesson_h="Sådan kører du en lektion",
    lesson=["Vis spillet på tavlen i 3 minutter: opret en profil, vælg \"Første skridt\", løs én bane sammen.",
            "Eleverne åbner <strong>codetrail.dk</strong>, skriver deres navn som profil og vælger samme sværhedsgrad.",
            "15-20 minutter på egen hånd. Knappen <strong>?</strong> på kortbakken forklarer kortene, pæren giver et hint.",
            "Afslut med at spørge: \"Hvem løste en bane med færrest kort? Hvordan?\"",
            "Næste gang: ét trin op i sværhedsgrad. \"Gæt målet\" er god som fælles opgave på tavlen."],
    practical_h="Praktisk",
    practical=["Fremskridt gemmes i browseren på den enkelte enhed. Deler eleverne enheder, kan en profil eksporteres som en kode under <em>Profiler</em> og hentes ind igen et andet sted.",
               "Ingen konti, ingen e-mail, intet sendes fra enheden. Læs mere: <a href=\"privacy.html\">privatlivspolitik</a>.",
               "Findes også som app til Windows, macOS, Linux og Android: <a href=\"https://github.com/acidsnare/CodeTrail/releases\">github.com/acidsnare/CodeTrail/releases</a>.",
               "Sprog skiftes under <em>Indstillinger</em>: dansk, engelsk, rumænsk, russisk, ukrainsk."],
    contact_h="Kontakt",
    contact="Spillet er lavet af en forælder i fritiden. Spørgsmål, ønsker og fejl: skriv gerne, eller opret en sag på <a href=\"https://github.com/acidsnare/CodeTrail/issues\">GitHub</a>.",
),
"ro": dict(
    title="CodeTrail la clasă",
    lead="Un joc gratuit în care elevii programează un mic erou până la țintă cu ajutorul cardurilor. Fără cont, fără instalare, fără colectare de date. Limba urmează dispozitivul și poate fi setată în <em>Setări</em>.",
    cta="Deschide jocul: codetrail.dk",
    facts=[("Vârsta", "Clasele pregătitoare - a IV-a (primele două niveluri de dificultate de la circa 5 ani)"),
           ("Discipline", "Informatică și TIC, matematică, activități opționale"),
           ("Durată", "20-45 de minute pe ședință, se poate repeta (nivelurile sunt mereu noi)"),
           ("Dispozitive", "Chromebook, PC, Mac sau iPad recent, în browser. Merge cu mouse și touch."),
           ("Preț", "Gratuit, fără reclame")],
    learn_h="Ce învață elevii?",
    learn=[("Gândire algoritmică:", "împărțirea unei sarcini în pași mici, în ordinea corectă."),
           ("Direcții și perspectivă:", "mai întâi sus/jos/stânga/dreapta, apoi \"întoarce la stânga\" din punctul de vedere al eroului."),
           ("Depanare:", "programul rulează, eroul greșește drumul, elevul găsește greșeala și o corectează."),
           ("Bucle și blocuri:", "\"repetă de 3 ori\" și un \"bloc A\" refolosibil la ultimele două niveluri."),
           ("Optimizare:", "mai multe stele pentru mai puține carduri, așa că elevii caută cea mai scurtă soluție.")],
    tiers_h="Niveluri de dificultate",
    tiers=[("1 Primii pași", "Săgeți sus, jos, dreapta, stânga. Drumuri scurte."),
           ("2 Drumuri lungi", "Aceleași săgeți, drumuri mai lungi și mai multe carduri de urmărit."),
           ("3 Întoarceri", "\"Înainte X\" și întoarcere la stânga / dreapta din perspectiva eroului."),
           ("4 Salturi", "Sărituri peste apă și pietre."),
           ("5 Bucle", "\"Repetă ×N\" cu carduri înăuntru. Table mai mari."),
           ("6 Blocuri", "Construiește \"blocul A\" o dată și cheamă-l de mai multe ori.")],
    modes="Două moduri: <strong>Construiește drumul</strong> (elevul scrie programul) și <strong>Ghicește finalul</strong> (elevul citește un program gata făcut și arată unde se oprește eroul). Al doilea antrenează citirea codului.",
    lesson_h="Cum decurge o lecție",
    lesson=["Arată jocul pe tablă 3 minute: creează un profil, alege \"Primii pași\", rezolvați un nivel împreună.",
            "Elevii deschid <strong>codetrail.dk</strong>, își scriu numele ca profil și aleg același nivel.",
            "15-20 de minute pe cont propriu. Butonul <strong>?</strong> de pe tava cu carduri explică fiecare card, becul dă un indiciu.",
            "Încheie întrebând: \"Cine a rezolvat un nivel cu cele mai puține carduri? Cum?\"",
            "Data viitoare: un nivel mai sus. \"Ghicește finalul\" merge bine ca sarcină comună pe tablă."],
    practical_h="Practic",
    practical=["Progresul se salvează în browser, pe fiecare dispozitiv. Dacă elevii împart dispozitivele, un profil poate fi exportat ca un cod din <em>Profiluri</em> și importat în altă parte.",
               "Fără conturi, fără e-mail, nimic nu părăsește dispozitivul. Detalii: <a href=\"privacy.html\">politica de confidențialitate</a>.",
               "Există și ca aplicație pentru Windows, macOS, Linux și Android: <a href=\"https://github.com/acidsnare/CodeTrail/releases\">github.com/acidsnare/CodeTrail/releases</a>.",
               "Limbi în <em>Setări</em>: daneză, engleză, română, rusă, ucraineană."],
    contact_h="Contact",
    contact="Jocul este făcut de un părinte în timpul liber. Întrebări, dorințe și erori: scrie-ne sau deschide un tichet pe <a href=\"https://github.com/acidsnare/CodeTrail/issues\">GitHub</a>.",
),
"ru": dict(
    title="CodeTrail в классе",
    lead="Бесплатная игра, в которой ученики программируют маленького героя карточками, чтобы довести его до цели. Без входа, без установки, без сбора данных. Язык следует за устройством, его можно выбрать в <em>Настройках</em>.",
    cta="Открыть игру: codetrail.dk",
    facts=[("Возраст", "0-4 классы (первые два уровня сложности примерно с 5 лет)"),
           ("Предметы", "Информатика, математика, дополнительные занятия"),
           ("Время", "20-45 минут за занятие, можно повторять (уровни всегда новые)"),
           ("Устройства", "Chromebook, ПК, Mac или новый iPad в браузере. Работает мышью и пальцем."),
           ("Цена", "Бесплатно, без рекламы")],
    learn_h="Чему учатся дети?",
    learn=[("Алгоритмическое мышление:", "разбить задачу на маленькие шаги в правильном порядке."),
           ("Направления и точка зрения:", "сначала вверх/вниз/влево/вправо, потом «повернуть налево» глазами героя."),
           ("Поиск ошибок:", "программа запускается, герой идёт не туда, ребёнок находит ошибку и исправляет."),
           ("Циклы и блоки:", "«повторить 3 раза» и многоразовый «блок A» на двух последних уровнях."),
           ("Оптимизация:", "больше звёзд за меньше карточек, поэтому дети ищут самое короткое решение.")],
    tiers_h="Уровни сложности",
    tiers=[("1 Первые шаги", "Стрелки вверх, вниз, вправо, влево. Короткие пути."),
           ("2 Длинные пути", "Те же стрелки, пути длиннее, карточек больше."),
           ("3 Повороты", "«Вперёд X» и повороты налево / направо глазами героя."),
           ("4 Прыжки", "Прыжки через воду и камни."),
           ("5 Циклы", "«Повторить ×N» с карточками внутри. Поле больше."),
           ("6 Блоки", "Собрать «блок A» один раз и вызывать его несколько раз.")],
    modes="Два режима: <strong>Собери путь</strong> (ребёнок пишет программу) и <strong>Угадай финиш</strong> (ребёнок читает готовую программу и показывает, где остановится герой). Второй тренирует чтение кода.",
    lesson_h="Как провести занятие",
    lesson=["Показать игру на доске 3 минуты: создать профиль, выбрать «Первые шаги», решить один уровень вместе.",
            "Дети открывают <strong>codetrail.dk</strong>, пишут своё имя как профиль и выбирают тот же уровень.",
            "15-20 минут самостоятельно. Кнопка <strong>?</strong> на лотке объясняет карточки, лампочка даёт подсказку.",
            "В конце спросить: «Кто решил уровень меньшим числом карточек? Как?»",
            "В следующий раз: на уровень выше. «Угадай финиш» хорошо идёт как общая задача на доске."],
    practical_h="Практика",
    practical=["Прогресс хранится в браузере на каждом устройстве. Если устройства общие, профиль можно выгрузить кодом в разделе <em>Профили</em> и загрузить в другом месте.",
               "Без аккаунтов, без e-mail, ничего не уходит с устройства. Подробнее: <a href=\"privacy.html\">политика конфиденциальности</a>.",
               "Есть и приложения для Windows, macOS, Linux и Android: <a href=\"https://github.com/acidsnare/CodeTrail/releases\">github.com/acidsnare/CodeTrail/releases</a>.",
               "Языки в <em>Настройках</em>: датский, английский, румынский, русский, украинский."],
    contact_h="Контакт",
    contact="Игру делает один родитель в свободное время. Вопросы, пожелания и ошибки: напишите или создайте issue на <a href=\"https://github.com/acidsnare/CodeTrail/issues\">GitHub</a>.",
),
"uk": dict(
    title="CodeTrail у класі",
    lead="Безкоштовна гра, у якій учні програмують маленького героя картками, щоб довести його до цілі. Без входу, без встановлення, без збору даних. Мова йде за пристроєм, її можна обрати в <em>Налаштуваннях</em>.",
    cta="Відкрити гру: codetrail.dk",
    facts=[("Вік", "0-4 класи (перші два рівні складності приблизно з 5 років)"),
           ("Предмети", "Інформатика, математика, додаткові заняття"),
           ("Час", "20-45 хвилин за заняття, можна повторювати (рівні завжди нові)"),
           ("Пристрої", "Chromebook, ПК, Mac або новий iPad у браузері. Працює мишею і пальцем."),
           ("Ціна", "Безкоштовно, без реклами")],
    learn_h="Чого вчаться діти?",
    learn=[("Алгоритмічне мислення:", "розбити задачу на маленькі кроки в правильному порядку."),
           ("Напрямки і точка зору:", "спочатку вгору/вниз/ліворуч/праворуч, потім «повернути ліворуч» очима героя."),
           ("Пошук помилок:", "програма запускається, герой іде не туди, дитина знаходить помилку і виправляє."),
           ("Цикли і блоки:", "«повторити 3 рази» та багаторазовий «блок A» на двох останніх рівнях."),
           ("Оптимізація:", "більше зірок за менше карток, тому діти шукають найкоротше рішення.")],
    tiers_h="Рівні складності",
    tiers=[("1 Перші кроки", "Стрілки вгору, вниз, праворуч, ліворуч. Короткі шляхи."),
           ("2 Довгі шляхи", "Ті самі стрілки, шляхи довші, карток більше."),
           ("3 Повороти", "«Вперед X» і повороти ліворуч / праворуч очима героя."),
           ("4 Стрибки", "Стрибки через воду і каміння."),
           ("5 Цикли", "«Повторити ×N» з картками всередині. Поле більше."),
           ("6 Блоки", "Зібрати «блок A» один раз і викликати його кілька разів.")],
    modes="Два режими: <strong>Збери шлях</strong> (дитина пише програму) і <strong>Вгадай фініш</strong> (дитина читає готову програму і показує, де зупиниться герой). Другий тренує читання коду.",
    lesson_h="Як провести заняття",
    lesson=["Показати гру на дошці 3 хвилини: створити профіль, обрати «Перші кроки», розв'язати один рівень разом.",
            "Діти відкривають <strong>codetrail.dk</strong>, пишуть своє ім'я як профіль і обирають той самий рівень.",
            "15-20 хвилин самостійно. Кнопка <strong>?</strong> на лотку пояснює картки, лампочка дає підказку.",
            "Наприкінці спитати: «Хто розв'язав рівень найменшою кількістю карток? Як?»",
            "Наступного разу: на рівень вище. «Вгадай фініш» добре йде як спільна задача на дошці."],
    practical_h="Практика",
    practical=["Прогрес зберігається в браузері на кожному пристрої. Якщо пристрої спільні, профіль можна вивантажити кодом у розділі <em>Профілі</em> і завантажити в іншому місці.",
               "Без акаунтів, без e-mail, нічого не йде з пристрою. Докладніше: <a href=\"privacy.html\">політика конфіденційності</a>.",
               "Є також застосунки для Windows, macOS, Linux і Android: <a href=\"https://github.com/acidsnare/CodeTrail/releases\">github.com/acidsnare/CodeTrail/releases</a>.",
               "Мови в <em>Налаштуваннях</em>: данська, англійська, румунська, російська, українська."],
    contact_h="Контакт",
    contact="Гру робить один з батьків у вільний час. Питання, побажання і помилки: напишіть або створіть issue на <a href=\"https://github.com/acidsnare/CodeTrail/issues\">GitHub</a>.",
),
}

TEMPLATE = """<!doctype html>
<html lang="{lang}">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>{title}</title>
  <link rel="icon" type="image/png" href="favicon.png">
{alternates}
  <style>
    body {{ margin: 0; background: #163B4B; color: #F2F2F2; font: 17px/1.6 system-ui, -apple-system, "Segoe UI", sans-serif; }}
    main {{ max-width: 720px; margin: 0 auto; padding: 32px 24px 64px; }}
    h1 {{ font-size: 32px; margin: 0 0 8px; }}
    h2 {{ font-size: 20px; margin: 32px 0 8px; }}
    .lead {{ font-size: 19px; opacity: .9; margin-bottom: 28px; }}
    a {{ color: #FFD54F; }}
    .langs {{ font-size: 14px; opacity: .8; margin-bottom: 24px; }}
    .langs a, .langs span {{ margin-right: 12px; }}
    .langs span {{ font-weight: 700; }}
    .cta {{ display: inline-block; background: #43A047; color: #fff; text-decoration: none; font-weight: 700; padding: 12px 24px; border-radius: 14px; margin: 8px 0 24px; }}
    table {{ border-collapse: collapse; width: 100%; }}
    td, th {{ text-align: left; padding: 6px 10px 6px 0; vertical-align: top; }}
    th {{ opacity: .7; font-weight: 600; white-space: nowrap; }}
    ul, ol {{ padding-left: 22px; }}
    li {{ margin: 4px 0; }}
    .box {{ background: rgba(255,255,255,.08); border-radius: 16px; padding: 16px 20px; margin: 16px 0; }}
    @media print {{ body {{ background: #fff; color: #111; }} a {{ color: #0a5; }} .box {{ background: #f2f2f2; }} .langs {{ display: none; }} }}
  </style>
</head>
<body>
<main>
  <nav class="langs">{langs}</nav>
  <h1>{title}</h1>
  <p class="lead">{lead}</p>

  <a class="cta" href="https://codetrail.dk/">{cta}</a>

  <table>
{facts}
  </table>

  <h2>{learn_h}</h2>
  <ul>
{learn}
  </ul>

  <h2>{tiers_h}</h2>
  <table>
{tiers}
  </table>
  <p>{modes}</p>

  <h2>{lesson_h}</h2>
  <div class="box">
    <ol>
{lesson}
    </ol>
  </div>

  <h2>{practical_h}</h2>
  <ul>
{practical}
  </ul>

  <h2>{contact_h}</h2>
  <p>{contact}</p>
</main>
</body>
</html>
"""


def render(lang: str) -> str:
    t = TEXT[lang]
    langs = " ".join(
        f"<span>{LABELS[l]}</span>" if l == lang else f'<a href="{PAGES[l]}">{LABELS[l]}</a>' for l in PAGES
    )
    alternates = "\n".join(f'  <link rel="alternate" hreflang="{l}" href="https://codetrail.dk/{PAGES[l]}">' for l in PAGES)
    rows = lambda pairs: "\n".join(f"    <tr><th>{k}</th><td>{v}</td></tr>" for k, v in pairs)
    return TEMPLATE.format(
        lang=lang, title=t["title"], lead=t["lead"], cta=t["cta"], langs=langs, alternates=alternates,
        facts=rows(t["facts"]),
        learn_h=t["learn_h"], learn="\n".join(f"    <li><strong>{k}</strong> {v}</li>" for k, v in t["learn"]),
        tiers_h=t["tiers_h"], tiers=rows(t["tiers"]), modes=t["modes"],
        lesson_h=t["lesson_h"], lesson="\n".join(f"      <li>{s}</li>" for s in t["lesson"]),
        practical_h=t["practical_h"], practical="\n".join(f"    <li>{s}</li>" for s in t["practical"]),
        contact_h=t["contact_h"], contact=t["contact"],
    )


if __name__ == "__main__":
    for lang, name in PAGES.items():
        (OUT / name).write_text(render(lang), encoding="utf-8")
        print("wrote", name)
