# <img src="FQ-HLL_App.png" width="30"/> FQ-HLL Keyboard 

<img src="FQ-HLL_Banner_Cropped.png"/>

## Description
The FQ-HLL (Frequency-Quantized HyperLogLog) keyboard is an Android keyboard, that uses the efficient and minimal memory usage [FQ-HLL Autocorrection](https://github.com/shun4midx/FQ-HyperLogLog-Autocorrect) algorithm, which [Shun](https://github.com/shun4midx) took part in creating to become a reality. Its main data structure was the cardinality estimator "HLL". More specifically, it uses certain properties of HLL's bit registers to simulate different frequencies without needing additional storage.

Of course, Shun's fascination over algorithms has inspired the FQ-HLL algorithm, since HLL is rarely used in this context. Yet, more importantly, he is also dyslexic, and has struggled for years never finding a fitting mobile keyboard for him. Autocorrection even by top companies rarely accounted for dyslexics, and oftentimes his typos would not be corrected, unless he manually corrects them himself. Surely, this frustration influenced his creation of the FQ-HLL Autocorrection algorithm --- it almost became a perfect depiction of how he reads as a dyslexic person. He found he did not require as much brainpower anymore to type with this keyboard due to the more dyslexia-friendly autocorrection suggestions, such as "klof" -> "folk".

Thus, here we present the FQ-HLL keyboard which uses the fast and low memory FQ-HLL Autocorrection algorithm. It's made for everyone but especially dyslexic people in mind, with an autocorrection algorithm that hopefully eases common frustrations with autocorrection. FQ-HLL does not require knowledge of any particular language, requiring only a dictionary to achieve high accuracy, which makes it perfect for multilingual users. The ability to have custom dictionaries would also be implemented in the future.

## Installation

Download the latest release of the app [here](https://github.com/shun4midx/FQ-HLL-Keyboard/releases/latest), install the apk and head to your phone's keyboard settings. Enable `FQ-HLL Keyboard` and switch to the keyboard.

Keyboard settings can be found by launching the FQ-HLL Keyboard app installed with the apk, or by pressing the `⎋` button on the keyboard.

Keyboard settings are saved across updates of the app.

## Themes

You can view the themes in the [themes folder](https://github.com/shun4midx/FQ-HLL-Keyboard/tree/main/themes#readme).

Want a custom theme? Customisable themes may be implemented in the future, but until then you can open an issue to request one. Please include hex codes of key colour, key text colour, key pressed colour, key border colour, and keyboard background colour. Alternatively, fork the repository, edit themes.xml, and either build your own apk or open a pull request.

## Features

- FQ-HLL autocorrect algorithm
- emoji/symbol support
- coyote-time-like handling of simutaneous key presses
- autocorrect/autocapitalization toggle
- lots of themes and theme customisability (key colour, key text colour, key pressed colour, key border colour, keyboard background colour)
- height customisation (short, medium, tall)
- text editor mode customisation (maximize, grid)
- multiple keyboard layouts (qwerty, azerty, qwertz, dvorak, colemak)

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
app/src/main
|-- AndroidManifest.xml
|-- cpp
|   |-- CMakeLists.txt
|   |-- FQ-HyperLogLog-Autocorrect (repo)
|   `-- native-lib.cpp
|-- ic_launcher-playstore.png
|-- java.com.fqhll.keyboard
|   |-- CustomKeyboardApp.java
|   |-- CustomKeyboardView.java
|   |-- MainActivity.kt
|   `-- Suggestion.java
`-- res
    |-- drawable
    |   |-- key_background.xml
    |   |-- key_popup_background.xml
    |   |-- key_popup_view.xml
    |   |-- key_pressed_background.xml
    |   `-- key_unpressed_background.xml
    |-- layout
    |   |-- activity_main.xml
    |   |-- custom_keyboard_layout.xml
    |   |-- custom_keyboard_preview.xml
    |   `-- spinner.xml
    `-- xml
        |-- clipboard.xml
        |-- custom_keypad_azerty.xml
        |-- custom_keypad_colemak.xml
        |-- custom_keypad_dvorak.xml
        |-- custom_keypad_medium.xml
        |-- custom_keypad_qwerty.xml
        |-- custom_keypad_qwertz.xml
        |-- custom_keypad_short.xml
        |-- custom_keypad_tall.xml
        |-- custom_method.xml
        |-- editor_grid.xml
        |-- editor_maximize.xml
        |-- emojis.xml
        |-- numpad.xml
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
- [x] clipboard
- [x] settings app layout
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
- [ ] bypass autocorrected word by long pressing space (long presses editor for now)
- [x] numpad
- [ ] add grid/maximize mode previews to readme
- [ ] changing non main keyboard height
- [x] text editor symbols
- [ ] fully finish text editor
- [ ] clipboard being able to access/use android copy key
- [x] add different keyboard layouts

## Contact

You can contact Shun via [Email](mailto:shun4midx@gmail.com) or Discord at @shun4midx, and Ducky via [Email](mailto:ducky4life@duck.com).
