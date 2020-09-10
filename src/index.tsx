import { NativeEventEmitter, NativeModules, Platform } from 'react-native';
import { request, PERMISSIONS, RESULTS } from 'react-native-permissions';

type PitchTrackerType = {
  prepare(): any;
  start(): any;
  stop(): any;
  noteOn(callback: (midiNum: any) => any): any;
  noteOff(callback: (midiNum: any) => any): any;
};

const { PitchTracker } = NativeModules;
const eventEmitter = new NativeEventEmitter(PitchTracker);

const askPermission = async () => {
  try {
    let permission = Platform.select({
      android: PERMISSIONS.ANDROID.RECORD_AUDIO,
      ios: PERMISSIONS.IOS.MICROPHONE,
    })!;
    const result = await request(permission);
    if (result === RESULTS.GRANTED) {
      console.log('Microphone Permission Successful');
    }
  } catch (error) {
    console.log('askPermission', error);
  }
};

export default {
  ...PitchTracker,
  prepare: () => {
    askPermission();
    PitchTracker.prepare();
  },
  noteOn: (callback: (res: object) => any) => {
    eventEmitter.addListener('NoteOn', callback);
  },
  noteOff: (callback: (res: object) => any) => {
    eventEmitter.addListener('NoteOff', callback);
  },
} as PitchTrackerType;
