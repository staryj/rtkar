#!/bin/sh

chmod +x ./adb > /dev/null 2>&1
xattr -d com.apple.quarantine ./adb > /dev/null 2>&1
export PATH=$PATH:.

if [[ ! -f "adb" || ! -r "adb" || ! -x "adb" ]]; then
echo .
echo Отсутствует файл adb, вы не разархивировали архив или не перешли в директорию скрипта
echo Разархивируйте архив и запустите этот скрипт ещё раз
echo .
read -p "Теперь это окно можно закрыть" -n1 -s
exit 1
fi

echo Подготовливаем adb к работе
echo .
echo Если тут зависло:
echo .
echo 1 Проверьте включен ли USB Debugging, https://voyahchat.ru/common/usb-debugging
echo Иногда помогает включить/выключить USB Debugging несколько раз
echo .
echo 2 Проверьте подходит ли кабель, https://voyahchat.ru/common/cable
echo На странице описаны все варианты использования кабеля, наиболее рабочий вариант кабель A-A
echo Кабель C-A не работает на Маке, нужно использовать кабель A-A и переходник C-A
echo Сначала вставляется переходник C-A в Мак, потом в него вставляется кабель A-A и вставляется в машину
echo .
echo 3 Перезагрузите компьютер и запустите скрипт заново
echo Проверьте, что у вас не держит ADB соединение что-то другое, например, установка Cunba
echo .
adb wait-for-device
if [ $? -ne 0 ]; then
echo .
echo Не удалось выполнить ADB соединение с машиной
echo Возможно у вас уже запущено какое-то другое ADB соединение
echo .
read -p "Теперь это окно можно закрыть" -n1 -s
exit 1
fi
echo adb к работе подготовлено

echo .
echo Устанавливаем RTKar
adb install -g rtkar.apk
if [ $? -ne 0 ]; then
echo .
echo Не удалось установить приложение
echo .
read -p "Теперь это окно можно закрыть" -n1 -s
exit 1
fi
echo RTKar установлено

echo .
read -p "Теперь это окно можно закрыть" -n1 -s
exit 0

