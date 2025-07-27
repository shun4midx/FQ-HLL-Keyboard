# FQ-HLL Keyboard

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
app/src/main
├── java/com.fqhll.keyboard
│   └── CustomKeyboardApp.java
├── cpp
│   └── native-lib.cpp
└── res
    ├── layout
    │   ├── custom_keyboard_layout.xml
    │   └── custom_keyboard_preview.xml
    ├── drawable
    │   └── key_background.xml
    └── xml
        ├── custom_keypad.xml
        ├── symbols.xml
        └── custom_method.xml
```

### latest development apk

https://github.com/shun4midx/FQ-HLL-Keyboard/blob/main/app/build/outputs/apk/debug/app-debug.apk

## latest stable apk

https://github.com/shun4midx/FQ-HLL-Keyboard/releases/latest
