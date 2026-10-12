# KlangDroid

KlangDroid ist die Android-App zum [KlangTresor](https://github.com/CasparDavi/klangtresor).
Sie spricht mit dem Tresor, der schon auf dem eigenen Rechner läuft, und bringt
Mediathek und Player aufs Handy: Titel, Playlists, und das Bewegtbild, das im
Tresor als Albumart neben dem Ton liegt.

Der KlangTresor bleibt, was er ist. Ein lokales Archiv der eigenen Musik, gebaut
von Caspar David. KlangDroid ist der mobile Raum dazu, gebaut von Tarja Ravenveil.
Die Songs, Texte und Artworks gehören ihren Urhebern und sind nicht Teil dieser
App.

- **Tarja Ravenveil**, Erstellerin von KlangDroid: [github.com/myinqi](https://github.com/myinqi)
- **Caspar David**, Ersteller des KlangTresors: [github.com/CasparDavi/klangtresor](https://github.com/CasparDavi/klangtresor)

## Was die erste Fassung kann

- Adresse des Tresors einstellen. Voreingestellt ist `http://localhost:8788/`.
- Mediathek aus `/api/index`, mit Suche, Sortierung und Playlists.
- Player für den Ton, der lokal im Tresor liegt (`audio.mp3`, auf Wunsch WAV
  oder eine eigene Fassung).
- Bewegtbild als Albumart: `artwork.mp4` oder das jüngste eigene Video
  (`eigen.mp4`, `eigen-2.mp4`, …), stumm und in Schleife, solange die Musik läuft.
- Farben wie im KlangTresor. **Rabe** ist die Voreinstellung (Violett über allem).
  Dazu Haus (Orange nach Magenta), Cover (die Fläche folgt dem laufenden Stück),
  Unbunt, und die Klangraum-Paletten Nebel, Eisnebel, Polarlicht, Mondlicht,
  Sonnenglut und Borg. Der Akzent lässt sich zusätzlich setzen.

## Lizenz

MIT. Siehe [LICENSE](LICENSE). Dieselbe Lizenz trägt der KlangTresor, und eine
Begleit-App soll sich genauso weitergeben, ändern und forken lassen. Die einzige
Bedingung: der Urhebervermerk reist mit.

Der KlangTresor selbst steht unter seiner eigenen MIT-Lizenz und bleibt das
Projekt von Caspar David. KlangDroid kopiert seinen Code nicht, sie redet über
die HTTP-Schnittstelle mit ihm.

## Verbinden

Der Tresor muss laufen (`node server/server.js` im KlangTresor, Port 8788).
In den Einstellungen der App steht die Adresse.

| Wo die App läuft | Adresse |
| --- | --- |
| Derselbe Rechner, oder ein Emulator mit `adb reverse tcp:8788 tcp:8788` | `http://localhost:8788/` |
| Android-Emulator ohne Reverse | `http://10.0.2.2:8788/` |
| Handy im Heimnetz | die Adresse, die der Tresor beim Start fürs Netz nennt, zum Beispiel `http://192.168.1.20:8788/` |

Der Tresor spricht im Heimnetz HTTP. Die App lässt Klartext deshalb zu.

## Bauen

Voraussetzung: JDK 17, Android SDK mit Platform 36.

```bash
export ANDROID_HOME="$HOME/android-sdk"
./gradlew :app:assembleDebug
```

Die Debug-APK liegt unter `app/build/outputs/apk/debug/`.

```bash
adb reverse tcp:8788 tcp:8788
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
