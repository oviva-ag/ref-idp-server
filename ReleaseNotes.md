# Release 21.0.1

- update gemLibPki
- add docker compose file

# Release 21.0.0

- Java version 17 mandatory

# Release 20.1.0

- IDP Federation Proof of Concept improvements

# Release 20.0.9

- use SecureRandom
- publish docker compose file for Idp-Server
- use Tiger test framework
- add IDP Federation Proof of Concept
- update dependencies
- clean code smells

# Release 19.3.0

- hot fix: exclude non existent dependency

# Release 19.2.0

- Dependencies aktualisiert
- Federation Master, Fachdienst und Sektoralen IDP mit ersten Endpunkten für PoC bereitgestellt
- Testsuite akzeptiert jetzt zusätzliche Scopes im Discovery Document und zusätzliche Schlüssel im JWKS
- IDP Client unterstützt jetzt den Claim "e-rezept-dev"
- tiger-test-lib version korrigiert

# Release 19.1.0

- Bereitstellung eines MVP für einen sektoralen IDP
- Unterstützung des Fast Track Flows im IDP Server
- Erweiterung der Testsuite um Testfälle für den Fast Track (gekennzeichnet über @FastTrack Annotation)
- Die vom sektoralen IDP bereitgestellten Token sind momentan noch statisch
- Der sektorale IDP bietet keine Authentisierungsmethode an, sondern liefert direkt den AUTHORIZATION_CODE aus
- Noch keine Tokenverschlüsslung auf der Strecke zwischen zentralem und sektoralem IDP
- Erweiterung eines Testfalls um weitere HBA-Ausprägungen
- Für SMC-Bs wird jetzt der commonName statt des organizationNames verwendet

# Release 18.1.0

- Fehlermeldungen in Testsuite überarbeitet
- Server wertet AMR aus
- Testsuite ergänzt um Test zum AMR, zum userAgent, zu abgelaufenen SSO-Token

# Release 18.0.0

- IDP-Client für ExternalizedSignature bei RSA erweitert
- AMR erweitert (Fix für Biometrie)
- Token-Flow Korrektur: Issuer wird korrekt befüllt

# Release 17.0.0

- Anpassungen bei Fehlermeldungen

# Release 16.0.0

- JWK-Koordinaten sind nun BigEndian kodiert
- Datentypen in Fehlermeldungen korrigiert

# Release 15.0.0

* Login mit AltAuth benötigt exp-Header in encrypted_signed_challenge
* Fehlercodes für altAuth-Endpunkte harmonisiert

# Release 14.0.0

* Korrigierte Fehlermeldungen
* MockClient kann AUD dynamisch befüllen
* DD hat Pairing als supported Scope
* Null-Json-Value-Handling in der Testsuite verbessert
* Option für Hex-Encoded symmetric-Keys in Test-Config
* RBeL-Flow Kommentare angepasst

# Release 13.0.1

* github pages link korrigiert

# Release 13.0.0

* aud in Access-Tokens wird nun korrekt gesetzt
* Verbessertes Flow-Rendering (HTML-Seiten durch RBeL-Logger)
* Testsuite speichert nun den Flow für jeden Testfall
* Erweiterte Konfigurierbarkeit der Testsuite
* Adaptierung SignedChallenge/Token encryption

# Release 12.0.0

* Korrekte Umsetzung des signed challenge flows
* Anpassung alternative Authentisierung CR/Datenformate Version 1.1.7
* Diverse Bugfixes im Basis- sowie Altauthflow
* IDP-Client erweitert zur Nutzung externer Schlüssel
* Zusätzliche Testfälle in der Testsuite für
    * Validierung von RSA-Zertifikaten als eGK
    * Validierung ungültiger Inhalte auf eGK
* Einbindung RBelLogger in Testsuite https://github.com/gematik/app-RbelLogger
* Konfigurierbare Variablen in der Testsuite
* Überarbeitung der IDP Fehlermeldungen (nicht final)
* Aktualisierung der Requirements auf Baseline für Testsuite

# Release 11.1.0

* GemLibPki-Version auf Version 0.4.1 aktualisiert

# Release 11.0.0

* Accept header werden vom IDP-Client benutzt sowie vom Server validiert
* Erstes Set von Testszenarien mit Anforderungen verlinkt
* NBF entfernt
* Endpunkt pairing in pairings umbenannt
* Korrekte Werte für not_after in Pairing-Data in der Testsuite
* Discoverydocument an neue Spezifikation angepasst
* IDP PUK_ENC ohne X5C in JWKS_URI Response
* Testsuite um Testfall für Primärsystem ohne SSO_TOKEN erweitert

# Release 10.0.0

* Fehlermeldungen spezifikationskonform im Basisflow
* CLAIMS ACR/NBF/AT_HASH/EXP/CTY spezifikationskonform
* Discovery Document überarbeitet
* AFO Reporter in Testsuite integriert

# Release 9.0.0

- Korrigierte redirect_uri
- Renaming ssotoken => sso_token
- kid in Header-Token enthalten
- kid-Werte im JWKS-Endpunkt und in einzelnen Schlüssel-Endpunkten vereinheitlicht
- Testfälle zur alternativen Authentisierung enthalten
- ACR-Wert angepasst
- jti im ID-Token ergänzt
- Versionranges für GemLibPki korrigiert

# Release 8.1.0

- User consent angepasst
- Validierung von device und pairing Daten implementiert
- Testsuite um Token Verschlüsselung für den Pairing Endpunkt erweitert
- gemlibPKI Version update -- fix https://github.com/gematik/ref-idp-server/issues/2

# Release 8.0.0

- Versionsnummer in den Response-Headern ersichtlich
- Aktuelles Bespiel für den TokenFlow unter /tokenFlow.html abrufbar
- Fix für lokales Discovery Document
- Biometrie Kommunikation verschlüsselt
- Draft Set Testfälle für Biometrie Registrierungsendpunkt
- Umstellung Schlüssel AUTH/TOKEN auf SIGN/ENC
- Einbindung der gemLibPKI
- ID-Token enthält nonce
- Fix für lokales Disc Doc in der Testsuite

# Release 7.0.0

RELEASE 7.0.0 IDP

- Es gibt einen TokenLoggerTest der die Tokens des Workflows in ein Dokument speichert (siehe idp-server/target)
- Prüfungen Tokengültigkeit
- diverse Tokenclaims angepasst
- Tokenverschlüsselung umgesetzt Testsuite
- Tokenverschlüsslung wird in der Testsuite geprüft (konfigurierbar)
- Beide Flows weitestgehend positiv und negativ getestet
- erste Testfälle zur Biometrie enthalten (Registrierung)
- Korrekturen zum Flow
- Abweichungen in der API konsolidiert

# Release 6.0.0

RELEASE 6.0.0

# Release 5.1.0

RELEASE 5.1.0 Zulassungstest Version 1.1.0

# Release 5.0.0

RELEASE 5.0.0 Zulassungstest Stufe 1

# Release 4.0.0

RELEASE 4.0.0

