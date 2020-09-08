import { NativeModules } from 'react-native';

type PitchTrackerType = {
  multiply(a: number, b: number): Promise<number>;
};

const { PitchTracker } = NativeModules;

export default PitchTracker as PitchTrackerType;
