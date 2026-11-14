# Публикация готового выпуска / Publishing a prepared release

Используется GitHub Actions, личный PAT и ключ подписи на runner не требуются. APK/SOURCE должны быть уже проверены и подписаны вне Actions.

1. Сохранить4готовых файла, release-notes.md и release.json в ветке release-files/ВЕРСИЯ. В manifest: repository, существующий tag, source_commit, release_id черновика, phone_version, title и files с name/size/sha256.
2. Зафиксировать точный40-значный commit пакета. Тег версии должен уже указывать на одобренный source_commit; его не перемещать.
3. GitHub → Actions → Publish verified release files → Run workflow: payload_commit — commit пакета, release_tag — существующий тег.
4. Процесс проверит пакет, загрузит недостающие файлы в существующий draft, проверит скачивание, опубликует pre-release и скачает все вложения ещё раз. Уже совпавшие файлы повторно не загружаются; расхождения/неизвестные вложения останавливают публикацию.
5. Независимо скачать4вложения и сверить с Drive. Обновить оба журнала/паспорт/ссылки и выполнить правила хранения. До проверки не объявлять релиз готовым.

Имя паспорта в Release и Drive — ASCII, например2.2.12-True-TV-Launcher-Passport.txt: GitHub очищает кириллицу в имени вложения. Текст паспорта остаётся русским.

Workflow запускается вручную, contents:write только у одного job, checkout закреплён на commit, persist-credentials=false. Нет автозапуска на push/PR, сборки APK, приватных ключей или пользовательских токенов. Source snapshot и APK после публикации не менять; поздняя документация отдельными коммитами.

Example: https://github.com/GokSssss/gTVlauncher/actions/runs/43558015933
