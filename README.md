# react-native-pitch-tracker

React Native Pitch Tracker implemented with Tensorflow Lite Model

- [x] iOS/iPadOS Implementation
- [ ] Android Implementation  

## Installation

```sh
npm install react-native-pitch-tracker
```

## Usage

### Prerequisites (XCode)
- First, Download [this(Download Link : Onsets and Frames TFLite)](https://storage.googleapis.com/magentadata/models/onsets_frames_transcription/tflite/onsets_frames_wavinput.tflite) Model.  
- After that, add this file to your XCode Workspace.
- Double check that the file is successfully imported to project.
  - Check the .tflite model is in your `Project File -> Build Phases -> Copy Bundle Resources`  

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
