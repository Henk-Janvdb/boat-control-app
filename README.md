# Boat Control SMS App

An Android APK application that allows you to control your boat via SMS commands.

## Features

- ✅ Add multiple SMS relay phone numbers
- ✅ Send ON command: `0000#ON#`
- ✅ Send OFF command: `0000#OFF#`
- ✅ Send custom SMS messages
- ✅ Simple and intuitive UI
- ✅ Manage phone numbers (add/remove)

## Requirements

- Android 5.0 (API 24) or higher
- SMS permissions enabled
- Active phone line for sending SMS

## Installation

1. Clone this repository
2. Open in Android Studio
3. Build and run on your device or emulator

## Usage

1. **Add SMS Relay Numbers**: Enter phone numbers where SMS commands will be relayed
2. **Send ON Command**: Click "ON" button to send `0000#ON#` to all added numbers
3. **Send OFF Command**: Click "OFF" button to send `0000#OFF#` to all added numbers
4. **Custom Messages**: Use custom SMS option for other commands

## Permissions

This app requires:
- `SEND_SMS` - To send SMS messages
- `READ_CONTACTS` - Optional, to select contacts

## Technical Details

- Built with Jetpack Compose
- Uses Android's SmsManager API
- Kotlin 1.9+
- Material Design 3

## License

MIT License - Feel free to use and modify