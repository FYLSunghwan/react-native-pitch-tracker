import { NativeEventEmitter, NativeModules, Platform } from 'react-native';
import { request, PERMISSIONS } from 'react-native-permissions';

type PitchTrackerType = {
  prepare(): any;
  start(): any;
  stop(): any;
  noteOn(callback: (midiNum: any) => any): any;
  noteOff(callback: (midiNum: any) => any): any;
};

const { PitchTracker } = NativeModules;
const eventEmitter = new NativeEventEmitter(PitchTracker);

export default {
  ...PitchTracker,
  prepare: () => {
    let permission = Platform.select({
      android: PERMISSIONS.ANDROID.ACCESS_FINE_LOCATION,
      ios: PERMISSIONS.IOS.LOCATION_ALWAYS,
    })!;
    request(permission);
    PitchTracker.prepare();
  },
  noteOn: (callback: (res: object) => any) => {
    eventEmitter.addListener('NoteOn', callback);
  },
  noteOff: (callback: (res: object) => any) => {
    eventEmitter.addListener('NoteOff', callback);
  },
} as PitchTrackerType;
