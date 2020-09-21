# react-native-pitch-tracker

[![NPM](https://nodei.co/npm/react-native-pitch-tracker.png?compact=true)](https://nodei.co/npm/react-native-pitch-tracker/)

React Native Pitch Tracker implemented with Tensorflow Lite Model

- [x] iOS/iPadOS Implementation
- [x] Android Implementation  

## Installation

```sh
npm install --save react-native-pitch-tracker react-native-permissions
```

## Usage

### Prerequisites (iOS/iPadOS)
- First, Download [this(Download Link : Onsets and Frames TFLite)](https://storage.googleapis.com/magentadata/models/onsets_frames_transcription/tflite/onsets_frames_wavinput_uni.tflite) Model.  
- After that, add this file to your XCode Project.
- Double check that the file is successfully imported to project.
  - Check the .tflite model is in your `Project File -> Build Phases -> Copy Bundle Resources`  
- Open your project's `Info.plist` in XCode, and add `NSMicrophoneUsageDescription` row.
  - Or in other editor, add this row in the plist.  
  ```plist
  <key>NSMicrophoneUsageDescription</key>
  <string>YOUR TEXT</string>
  ```
- Open your project's `Podfile` and update with these lines.
```ruby
target 'YourAwesomeProject' do

  # …

  permissions_path = '../node_modules/react-native-permissions/ios'

  pod 'Permission-Microphone', :path => "#{permissions_path}/Microphone.podspec"

end
```  

### Prerequisites (Android)
- Copy the downloaded [file(tflite model)](https://storage.googleapis.com/magentadata/models/onsets_frames_transcription/tflite/onsets_frames_wavinput_uni.tflite) to `{ProjDirectory}/android/app/src/main/assets`.
- After that, update the `build.gradle`
```gradle
android {

    // …

    aaptOptions {
        noCompress "tflite"
    }
}
```  


### Usage in React Native Code

```js
import PitchTracker from "react-native-pitch-tracker";

// ...

// Must do before start()  
PitchTracker.prepare()

// Event Subscription (Add function to parameter)
PitchTracker.noteOn((res) => {
    console.log('Note On: ' + res['midiNum']);
}); // Note On: 60
PitchTracker.noteOff((res) => {
    console.log('Note Off: ' + res['midiNum']);
}); // Note Off: 60

// Start PitchTracker Engine
PitchTracker.start()

// Stop PitchTracker Engine
PitchTracker.stop()
```

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT
