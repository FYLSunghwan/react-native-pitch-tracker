import { NativeEventEmitter, NativeModules } from 'react-native';

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
  noteOn: (callback: (res: object) => any) => {
    eventEmitter.addListener('NoteOn', callback);
  },
  noteOff: (callback: (res: object) => any) => {
    eventEmitter.addListener('NoteOff', callback);
  },
} as PitchTrackerType;
