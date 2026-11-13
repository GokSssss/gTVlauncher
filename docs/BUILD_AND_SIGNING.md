# Восстановление сборки True TV Launcher

Используемый комплект: https://drive.google.com/drive/folders/1oBrhp7hFTO7c5bMW0qmCwBFj4-MBfeCo — Ресурсы/Сборка и подпись. Содержит фактически применяемый true-tv.p12, «Параметры сборки и подписи.txt» с alias/реальными паролями/инструкцией и прежний неизменный резерв того же ключа. Параметры: https://drive.google.com/file/d/105RGb87OvZAFpLFJT4k2oOOTtb3wUycr/view . Ключ: https://drive.google.com/file/d/1-zbWMpC7Te3OkZ2wwv5IBAm7-bubFZMp/view .

Распаковать точный SOURCE, создать `.signing/` рядом с tools/, скачать ключ в `.signing/true-tv.p12`, взять пароль из файла параметров и записать одной строкой в `.signing/password`. Выполнить `python3 tools/build.py` для ТВ или `python3 tools/build-phone.py` для телефона. Зависимости публичные, URL/SHA закреплены в tools/bootstrap.py; кэш может быть получен заново. Java/Python3 необходимы. Приватные материалы в SOURCE/APK не входят; ключ не заменять новым.

Сертификат прежних и текущих APK SHA256 a5a632bd6a8b8e2c3e3623ee245c3a5c3652c0c6b32751a35a49d6a9c2e8fa33, alias truetv, PKCS12, v1/v2.10.11.2026 архив ранее сохранённого ключа, прямой ключ и параметры скачаны/сверены; скачанный ключ открыт с паролем из скачанного файла параметров, сертификат совпал. Папка перемещена по новым общим правилам10.11.2026 с сохранением ID и прав (shared=false), никаких изменений ACL/sharing.
