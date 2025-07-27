# FQ-HLL Keyboard

<img src="FQ-HLL_Banner_Cropped.png"/>

## Installation

Download the latest release of the app [here](https://github.com/shun4midx/FQ-HLL-Keyboard/releases/latest), install the apk and head to your phone's keyboard settings. Enable `FQ-HLL Keyboard` and switch to the keyboard.

Keyboard settings can be found by launching the FQ-HLL Keyboard app installed with the apk.

## Development usage

setup android studio and link to phone

run the main app, then head to phone settings and search for keyboard

enable `FQ-HLL Keyboard` in the settings

switch to the keyboard in a text editor

### Keyboard files

`CustomKeyboardApp.java`: the actual code of the keyboard

`native-lib.cpp`: how the app starts

`custom_keyboard_layout.xml`: constructor of the keyboard

`custom_keyboard_preview.xml`: the layout of the popup when holding down a key

`custom_keypad.xml`: the entire layout of the keyboard

`custom_method.xml`: method for the keyboard

`key_background.xml`: the background of the keyboard

`symbols.xml`: the symbols tab of the keyboard

```
app/src/main/
|-- AndroidManifest.xml
|-- cpp
|   |-- CMakeLists.txt
|   `-- native-lib.cpp
|-- java/com.fqhll.keyboard
|    |-- CustomKeyboardApp.java
|     `-- MainActivity.kt
`-- res
    |-- drawable
    |   |-- key_background.xml
    |   |-- key_popup_background.xml
    |   `-- key_popup_view.xml
    |-- layout
    |   |-- activity_main.xml
    |   |-- custom_keyboard_layout.xml
    |   |-- custom_keyboard_layout_default.xml
    |   |-- custom_keyboard_preview.xml
    |   `-- spinner.xml
    |-- values
    |   |-- attrs.xml
    |   |-- colors.xml
    |   |-- strings.xml
    |   `-- themes.xml
    |-- values-night
    |   `-- themes.xml
    `-- xml
        |-- backup_rules.xml
        |-- custom_keypad.xml
        |-- custom_method.xml
        `-- symbols.xml
```

### latest development apk

might not work, but you get the latest stuff

prereleases are good beta versions of the app but are somewhat more tested

https://github.com/shun4midx/FQ-HLL-Keyboard/blob/main/app/build/outputs/apk/debug/app-debug.apk

## Contact

You can contact Shun via [Email](mailto:shun4midx@gmail.com) or Discord at @shun4midx, and Ducky via [Email](mailto:ducky4life@duck.com).