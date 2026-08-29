# Voice Ludo App (Kotlin / Jetpack Compose)

Yeh project asal `ludo_final_v3-2-2-1-1-2.html` (Ludo game) aur `index.html`
(Voice Party login/lobby) ka **native Kotlin rewrite** hai — HTML/JS/CSS
istemal nahi hua, sab kuch Jetpack Compose se render hota hai.

## GitHub par kaise chalayen

1. Is poore folder ko ek naye GitHub repo mein push kar dein (`git init`,
   `git add .`, `git commit`, phir apne repo mein push).
2. Repo ke **Actions** tab mein jayein — `.github/workflows/android-build.yml`
   khud chal jayega (push par ya "Run workflow" button se).
3. Build khatam hone par **Actions → is workflow run → Artifacts** mein
   `voice-ludo-debug-apk` milegi — download kar ke apne phone par install
   kar sakte hain.

## Abhi kya taiyar hai

- **Ludo game (Kotlin/Compose)**: 15x15 board, 4 colored yards, dice, token
  move, capture, safe cells, Classic/Arrow/Quick mode ke core rules
  - 4-player mein 1st/2nd/3rd rank badge, game last player tak jaari rehta hai
  - Quick mode: jeetne wale ke baqi tokens khud yard mein wapis chale jate hain
  - Filhal 3 baqi players "bot" ki tarah khud-b-khud khelte hain (pass-and-play)
- **Voice Party**: Main login screen (Facebook/Mobile/Gmail buttons, asal
  jaisay gradients/colors), simplified Mobile-login form, aur ek chhota
  home/lobby screen jahan se Ludo khola ja sakta hai

## Abhi kya baqi hai (agla iteration)

Asal `index.html` mein 15+ screens hain — yeh sab abhi tak native nahi hue:

- Facebook / Gmail login ke pure flows, Signup (mobile/gmail), Forgot password
- Poori 200+ country list wala mobile-login dropdown
- Settings screen, Profile edit screen, Store screen, Payment screen
- Master mode ke khaas magic-cell mechanics (Ludo)

Yeh sab batayen ke kis tarteeb mein chahiye, mai unhe isi project mein
add karta jaunga.
