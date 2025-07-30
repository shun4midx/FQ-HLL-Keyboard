# <img src="FQ-HLL_App.png" width="30"/> FQ-HLL Keyboard 

<img src="FQ-HLL_Banner_Cropped.png"/>

## Installation

Download the latest release of the app [here](https://github.com/shun4midx/FQ-HLL-Keyboard/releases/latest), install the apk and head to your phone's keyboard settings. Enable `FQ-HLL Keyboard` and switch to the keyboard.

Keyboard settings can be found by launching the FQ-HLL Keyboard app installed with the apk.

Keyboard settings are saved across updates of the app.

## Development usage

setup android studio and link to phone

run the main app, then head to phone settings and search for keyboard

enable `FQ-HLL Keyboard` in the settings

switch to the keyboard in a text editor

### Keyboard files

`java/com.fqhll.keyboard`: the actual code of the keyboard

`native-lib.cpp`: how the app starts

`activity_main.xml`: the initial layout of the app

`custom_keyboard_preview.xml`: the layout of the popup when holding down a key

`custom_keypad.xml`: the entire layout of the keyboard

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
|    |-- CustomKeyboardView.java
|     `-- MainActivity.kt
`-- res
    |-- drawable
    |   |-- key_background.xml
    |   |-- key_popup_background.xml
    |   `-- key_popup_view.xml
    |-- layout
    |   |-- activity_main.xml
    |   |-- custom_keyboard_layout.xml
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
        |-- emojis.xml
        `-- symbols.xml
```

### latest development apk

might not work, but you get the latest stuff

prereleases are good beta versions of the app but are somewhat more tested

https://github.com/shun4midx/FQ-HLL-Keyboard/blob/main/app/build/outputs/apk/debug/app-debug.apk

### todo

- [ ] customise symbols
- [ ] fully customise themes
- [x] emoji support (emoji page like symbols?)
- [ ] clipboard
- [ ] settings app layout
- [ ] hold keys for symbols
- [ ] multiple languages support?
- [ ] customise keyboard height
- [ ] custom background image
- [ ] key opacity
- [ ] custom key preview colour
- [ ] figure out a way to build gradle project using github actions
- [ ] fix unsigned release apk not working
- [ ] sign apk
- [ ] second page of symbols
- [x] be able to delete highlighted text
- [x] load changed themes without needing a refresh of keyboard (and without breaking popup/preview)
- [x] coyote-time-like queueing of handling simutaneous key presses
- [ ] an easy way to add word to dictionary
- [ ] emoji suggestions in place of predictive text
- [ ] bypass autocorrected word by long pressing space

## Contact

You can contact Shun via [Email](mailto:shun4midx@gmail.com) or Discord at @shun4midx, and Ducky via [Email](mailto:ducky4life@duck.com).
